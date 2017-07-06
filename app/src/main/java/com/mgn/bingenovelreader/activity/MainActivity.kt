package com.mgn.bingenovelreader.activity

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.graphics.drawable.DrawableCompat
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.database.getNovel
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.event.EventType
import com.mgn.bingenovelreader.event.NovelEvent
import com.mgn.bingenovelreader.fragment.DownloadFragment
import com.mgn.bingenovelreader.fragment.LibraryFragment
import com.mgn.bingenovelreader.fragment.SearchFragment
import com.tapadoo.alerter.Alerter
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode


class MainActivity : AppCompatActivity(), BottomNavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        navigation.setOnNavigationItemSelectedListener(this)
        navigation.selectedItemId = R.id.navigation_library
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.navigation_library -> {
                replaceFragment(LibraryFragment(), "LibraryFragment")
                return true
            }
            R.id.navigation_search -> {
                replaceFragment(SearchFragment(), "SearchFragment")
                return true
            }
            R.id.navigation_downloads -> {
                replaceFragment(DownloadFragment(), "downloadFragment")
                return true
            }
        }
        return false
    }

    fun replaceFragment(fragment: Fragment, tag: String) {
        val existingFrag = supportFragmentManager.findFragmentByTag(tag)
        var replaceFrag = fragment
        if (existingFrag != null) {
            replaceFrag = existingFrag
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, replaceFrag, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    override fun onBackPressed() {
        finish()
    }

    // region Event Bus
    override fun onStart() {
        super.onStart()
        EventBus.getDefault().register(this)
    }

    override fun onStop() {
        EventBus.getDefault().unregister(this)
        super.onStop()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onNovelEvent(event: NovelEvent) {
        if (event.type == EventType.COMPLETE) {
            val novel = dbHelper.getNovel(event.novelId)
            if (novel != null) {
                val alerter = Alerter.create(this)
                    .setTitle(novel.name)
                    .setText("is up-to-date ｡◕ ‿ ◕｡")
                    .setIcon(R.drawable.ic_book_black_vector)
                    .setBackgroundColor(R.color.Orange)
                    .setDuration(5000)
//                if (novel.imageFilePath != null) {
//                    val options = BitmapFactory.Options()
//                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
//                    try {
//                        val bitmap = BitmapFactory.decodeStream(FileInputStream(File(novel.imageFilePath)), null, options)
//                        alerter.setIcon(bitmap)
//                    } catch (e: Exception) {
//                        e.printStackTrace()
//                    }
//                }
                alerter.show()
            }
        }
    }
    //endregion

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val drawable = DrawableCompat.wrap(menu?.findItem(R.id.action_settings)!!.icon)
        DrawableCompat.setTint(drawable, ContextCompat.getColor(this, R.color.white))
        menu.findItem(R.id.action_sync)?.icon = drawable
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.action_settings)
            startSettingsActivity()
        return super.onOptionsItemSelected(item)
    }

    private fun startSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

}
