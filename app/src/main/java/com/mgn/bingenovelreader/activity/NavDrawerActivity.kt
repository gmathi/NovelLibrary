package com.mgn.bingenovelreader.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentTransaction
import android.support.v4.content.ContextCompat
import android.support.v4.view.GravityCompat
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.fragment.DownloadFragment
import com.mgn.bingenovelreader.fragment.LibraryFragment
import com.mgn.bingenovelreader.fragment.SearchFragment
import com.mgn.bingenovelreader.util.Utils
import kotlinx.android.synthetic.main.activity_nav_drawer.*

class NavDrawerActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_drawer)
        navigationView.setNavigationItemSelectedListener(this)

        if (Utils.checkNetwork(this))
            loadFragment(R.id.nav_search)
        else
            loadFragment(R.id.nav_library)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            finish()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> drawerLayout.openDrawer(GravityCompat.START)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        loadFragment(item.itemId)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    fun loadFragment(id: Int) {
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
        }
    }


    fun replaceFragment(fragment: Fragment, tag: String) {
        val existingFrag = supportFragmentManager.findFragmentByTag(tag)
        var replaceFrag = fragment
        if (existingFrag != null) {
            replaceFrag = existingFrag
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.navFragmentContainer, replaceFrag, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    fun setToolbar(toolbar: Toolbar?) {
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val indicator = ContextCompat.getDrawable(this, R.drawable.ic_menu_black_vector)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            indicator.mutate().setTint(ContextCompat.getColor(this, R.color.white))
        }
        supportActionBar?.setHomeAsUpIndicator(indicator)
    }

    private fun startSettingsActivity() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }


}
