package io.github.gmathi.novellibrary.activity

import android.app.Fragment
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.widget.TextView
import com.yarolegovich.slidingrootnav.SlideGravity
import com.yarolegovich.slidingrootnav.SlidingRootNav
import com.yarolegovich.slidingrootnav.SlidingRootNavBuilder
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.DrawerAdapter
import io.github.gmathi.novellibrary.fragment.CenteredTextFragment
import io.github.gmathi.novellibrary.model.DrawerItem
import io.github.gmathi.novellibrary.model.SimpleItem
import io.github.gmathi.novellibrary.model.SpaceItem
import java.util.*

/**
 * Created by a6001823 on 11/13/17.
 */
class NewReaderPagerActivity : AppCompatActivity(), DrawerAdapter.OnItemSelectedListener{

    private var slidingRootNav: SlidingRootNav? = null
    lateinit var recyclerView: RecyclerView

    private val POS_DASHBOARD = 0
    private val POS_ACCOUNT = 1
    private val POS_MESSAGES = 2
    private val POS_CART = 3
    private val POS_LOGOUT = 5

    private var screenTitles: Array<String>? = null
    lateinit var screenIcons: Array<Drawable?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_reader_pager)

        val cardview = findViewById<TextView>(R.id.menuTextView)
        slidingRootNav = SlidingRootNavBuilder(this)
                .withMenuOpened(false)
                .withContentClickableWhenMenuOpened(true)
                .withSavedState(savedInstanceState)
                .withGravity(SlideGravity.RIGHT)
                .withMenuLayout(R.layout.menu_left_drawer)
                .inject()

        cardview.setOnClickListener {
            toogleSlideRootNav()
        }

        screenIcons = loadScreenIcons()
        screenTitles = loadScreenTitles()
        val adapter = DrawerAdapter(Arrays.asList(
                createItemFor(POS_DASHBOARD).setChecked(true),
                createItemFor(POS_ACCOUNT).setSwitchOn(true),
                createItemFor(POS_MESSAGES),
                createItemFor(POS_CART),
                SpaceItem(48),
                createItemFor(POS_LOGOUT)) as List<DrawerItem<DrawerAdapter.ViewHolder>>)
        adapter.setListener(this)


        val list = findViewById<RecyclerView>(R.id.list)
        list.isNestedScrollingEnabled = false
        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        adapter.setSelected(POS_DASHBOARD)
    }



    private fun createItemFor(position: Int): DrawerItem<SimpleItem.ViewHolder> {
        return SimpleItem(screenIcons[position]!!, screenTitles!![position])
                .withIconTint(R.color.textColorSecondary)
                .withTextTint(R.color.textColorPrimary)
                .withSelectedIconTint(R.color.colorAccent)
                .withSelectedTextTint(R.color.colorAccent)
    }
    fun toogleSlideRootNav() {
        if (slidingRootNav!!.isMenuOpened)
            slidingRootNav!!.closeMenu()
        else
            slidingRootNav!!.openMenu()
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
    private fun showFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
    }
    override fun onItemSelected(position: Int) {
        if (position == POS_LOGOUT) {
            finish()
        }
        slidingRootNav!!.closeMenu()
        val selectedScreen = CenteredTextFragment.createFor(screenTitles!![position])
        showFragment(selectedScreen)
    }
}
