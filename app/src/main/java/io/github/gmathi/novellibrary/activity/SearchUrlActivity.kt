package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import android.view.MenuItem
import androidx.fragment.app.commit
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.databinding.ActivitySearchResultsBinding
import io.github.gmathi.novellibrary.fragment.SearchUrlFragment

class SearchUrlActivity : BaseActivity() {
    
    private lateinit var binding: ActivitySearchResultsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivitySearchResultsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Search: ${intent.getStringExtra("title")}"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        val url = intent.getStringExtra("url") ?: return
        val fragment = SearchUrlFragment.newInstance(url)
        replaceFragment(fragment, "SearchUrlFragment")
    }

    private fun replaceFragment(fragment: Fragment, tag: String) {
        val existingFrag = supportFragmentManager.findFragmentByTag(tag)
        var replaceFrag = fragment
        if (existingFrag != null) {
            replaceFrag = existingFrag
        }

        supportFragmentManager.commit(true) {
            replace(binding.contentSearchResults.fragmentContainer.id, replaceFrag, tag)
            setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE)
            addToBackStack(tag)
        }
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}
