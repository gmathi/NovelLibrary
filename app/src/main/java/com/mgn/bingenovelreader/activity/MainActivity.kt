package com.mgn.bingenovelreader.activity

import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
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
                Alerter.create(this)
                    .setTitle(novel.name)
                    .setText("is up-to-date ｡◕ ‿ ◕｡")
                    .setIcon(R.drawable.ic_book_black_vector)
                    .setBackgroundColor(R.color.Orange)
                    .setDuration(5000)
                    .show()
            }
        }
    }
    //endregion

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return super.onOptionsItemSelected(item)
    }

}
