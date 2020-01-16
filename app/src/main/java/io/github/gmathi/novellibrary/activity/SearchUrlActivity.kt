package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.fragment.SearchUrlFragment
import kotlinx.android.synthetic.main.activity_search_results.*

class SearchUrlActivity : BaseActivity() {

    private var fragment: Fragment? = null
    private lateinit var url: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_results)
        setSupportActionBar(toolbar)
        supportActionBar?.title = "Search: ${intent.getStringExtra("title")}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        url =
                if (savedInstanceState != null && savedInstanceState.containsKey("url"))
                    savedInstanceState.getString("url")!!
                else
                    intent.getStringExtra("url")!!

        val fragment =
                when {
                    fragment != null -> fragment!!
                    savedInstanceState == null -> SearchUrlFragment.newInstance(url)
                    else -> restoreFragment(savedInstanceState, "SearchUrlFragment") ?: SearchUrlFragment.newInstance(url)
                }
        replaceFragment(fragment, "SearchUrlFragment")
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        this.fragment = fragment
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment, tag)
            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            .addToBackStack(tag)
            .commitAllowingStateLoss()
    }

    private fun restoreFragment(savedInstanceState: Bundle, tag: String): Fragment? {
        val fragment =  supportFragmentManager.getFragment(savedInstanceState, tag)
        if (fragment != null)
            this.fragment = fragment
        return fragment
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("url", url)
        if (fragment != null)
            supportFragmentManager.putFragment(outState, "SearchUrlFragment", fragment!!)
    }

}
