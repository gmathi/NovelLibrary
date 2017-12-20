package io.github.gmathi.novellibrary.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateNewChapterCount
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.fragment.DownloadFragment
import io.github.gmathi.novellibrary.fragment.LibraryFragment
import io.github.gmathi.novellibrary.fragment.SearchFragment
import io.github.gmathi.novellibrary.model.DrawerItem
import io.github.gmathi.novellibrary.model.ReaderMenu
import io.github.gmathi.novellibrary.model.SimpleItem
import io.github.gmathi.novellibrary.model.SpaceItem
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.android.synthetic.main.activity_nav_drawer.*
import org.cryse.widget.persistentsearch.PersistentSearchView
import java.util.*

@SuppressLint("Registered")
class NewDrawerActivity : AppCompatActivity(), DrawerAdapter.OnItemSelectedListener, SimpleItem.Listener<ReaderMenu> {
    override fun bind(item: ReaderMenu, itemView: View, position: Int, simpleItem: SimpleItem) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


    private var slidingRootNav: SlidingRootNav? = null
    lateinit var recyclerView: RecyclerView

    private val posSearch = 0
    private val posLibrary = 1
    private val posDownloads = 2
    private val posSettings = 3
    private val posQuickAccess = 4
    private val posUpdateNovels = 5
    private val posOther = 6
    private val posJoinOnDiscord = 7

    private var screenTitles: Array<String>? = null
    lateinit var screenIcons: Array<Drawable?>

    private var currentNavId: Int = R.id.nav_search
    private var snackBar: Snackbar? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reader_pager)

        slideMenuSetup(savedInstanceState)

        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        slideMenuAdapterSetup()
        currentNavId = if (dataCenter.loadLibraryScreen) R.id.nav_library else R.id.nav_search

        if (intent.hasExtra("currentNavId"))
            currentNavId = intent.getIntExtra("currentNavId", currentNavId)

        if (savedInstanceState != null) {
            currentNavId = savedInstanceState.getInt("currentNavId")
        }

        if (intent.extras != null && intent.extras.containsKey("novelsChapMap")) {
            @Suppress("UNCHECKED_CAST")
            val novelsMap = intent.extras.getSerializable("novelsChapMap") as HashMap<String, Long>
            novelsMap.keys.forEach {
                val novel = dbHelper.getNovel(it)
                if (novel != null) {
                    dbHelper.updateNewChapterCount(novel.id, novelsMap[it]!!)
                }
            }
        }

        loadFragment(currentNavId)
    }

    private fun slideMenuSetup(savedInstanceState: Bundle?) {
        slidingRootNav = SlidingRootNavBuilder(this)
            .withMenuOpened(false)
            .withContentClickableWhenMenuOpened(true)
            .withSavedState(savedInstanceState)
            .withGravity(SlideGravity.LEFT)
            .withMenuLayout(R.layout.menu_left_drawer)
            .inject()
    }

    private fun slideMenuAdapterSetup() {
        @Suppress("UNCHECKED_CAST")
        val adapter = DrawerAdapter(Arrays.asList(
            createItemFor(posSearch).setChecked(true),
            createItemFor(posLibrary),
            createItemFor(posDownloads),
            createItemFor(posSettings),
            createItemForTextOnly(posQuickAccess),
            createItemFor(posUpdateNovels),
            createItemForTextOnly(posOther),
            createItemFor(posJoinOnDiscord)) as List<DrawerItem<DrawerAdapter.ViewHolder>>)
        adapter.setListener(this)


        val list = findViewById<RecyclerView>(R.id.list)
        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        adapter.setSelected(posSearch)
    }

    private fun createItemForTextOnly(position: Int): DrawerItem<SpaceItem.ViewHolder> {
        return SpaceItem(screenTitles!![position])
            .withTextTint(R.color.textColorPrimary)


    }

    private fun createItemFor(position: Int): DrawerItem<SimpleItem.ViewHolder> {

        return SimpleItem(ReaderMenu(screenIcons[position]!!, screenTitles!![position]), this)
            .withIconTint(R.color.textColorSecondary)
            .withTextTint(R.color.textColorPrimary)
            .withSelectedIconTint(R.color.colorAccent)
            .withSelectedTextTint(R.color.colorAccent)
    }

    private fun loadScreenTitles(): Array<String> {
        return resources.getStringArray(R.array.ld_activityScreenTitles)
    }

    private fun loadScreenIcons(): Array<Drawable?> {
        val ta = resources.obtainTypedArray(R.array.ld_activityScreenIcons)
        val icons = arrayOfNulls<Drawable>(ta.length())
        for (i in 0 until ta.length()) {
            val id = ta.getResourceId(i, 0)
            if (id != 0) {
                icons[i] = ContextCompat.getDrawable(this, id)
            }
        }
        ta.recycle()
        return icons
    }

    override fun onItemSelected(position: Int) {
        if (position == posJoinOnDiscord) {
            finish()
        }
        slidingRootNav!!.closeMenu()
        when (position) {
            posSearch -> {
                loadFragment(R.id.nav_search)
            }
            posLibrary -> {
                loadFragment(R.id.nav_library)
            }
            posDownloads -> {
                loadFragment(R.id.nav_downloads)
            }
            posSettings -> {
                loadFragment(R.id.nav_settings)
            }
            posUpdateNovels -> {
                loadFragment(R.id.nav_recently_updated)
            }
            posJoinOnDiscord -> {
                loadFragment(R.id.nav_discord_link)
            }
        }
    }

    private fun loadFragment(id: Int) {
        currentNavId = id
        when (id) {
            R.id.nav_library -> {
                replaceFragment(LibraryFragment(), LibraryFragment::class.toString())
            }
            R.id.nav_search -> {
                replaceFragment(SearchFragment(), SearchFragment::class.toString())
            }
            R.id.nav_downloads -> {
                replaceFragment(DownloadFragment(), DownloadFragment::class.toString())
            }
            R.id.nav_settings -> {
                startSettingsActivity()
            }
//            R.id.nav_recently_viewed -> {
//                startRecentlyViewedNovelsActivity()
//            }
            R.id.nav_recently_updated -> {
                startRecentlyUpdatedNovelsActivity()
            }
            R.id.nav_discord_link -> {
                openInBrowser("https://discord.gg/g2cQswh")
            }
        }
    }

    private fun replaceFragment(fragment: android.support.v4.app.Fragment, tag: String) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val existingSearchFrag = supportFragmentManager.findFragmentByTag(SearchFragment::class.toString())
            if (existingSearchFrag != null) {
                val searchView = existingSearchFrag.view?.findViewById<PersistentSearchView>(R.id.searchView)
                if (searchView != null && (searchView.isEditing || searchView.isSearching)) {
                    (existingSearchFrag as SearchFragment).closeSearch()
                    return
                }
            }
            val existingDownloadFrag = supportFragmentManager.findFragmentByTag(DownloadFragment::class.toString())
            if (existingDownloadFrag != null) {
                loadFragment(R.id.nav_library)
                return
            }

            if (snackBar != null && snackBar!!.isShown)
                finish()
            else {
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Constants.OPEN_DOWNLOADS_RES_CODE) {
            loadFragment(R.id.nav_downloads)
        }
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putInt("currentNavId", currentNavId)
    }


}
