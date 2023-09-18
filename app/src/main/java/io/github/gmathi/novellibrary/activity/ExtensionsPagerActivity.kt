package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.ExtensionsPageListener
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.databinding.ActivityExtensionsPagerBinding

class ExtensionsPagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityExtensionsPagerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityExtensionsPagerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setViewPager()
    }

    private fun setViewPager() {
        while (supportFragmentManager.backStackEntryCount > 0) supportFragmentManager.popBackStack()

        val navPageAdapter = GenericFragmentStatePagerAdapter(
            supportFragmentManager,
            arrayOf(getString(R.string.title_fragment_sources), getString(R.string.title_fragment_extensions)),
            2,
            ExtensionsPageListener()
        )

        binding.content.viewPager.offscreenPageLimit = 3
        binding.content.viewPager.adapter = navPageAdapter
        binding.content.tabStrip.setViewPager(binding.content.viewPager)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> finish()
        }
        return super.onOptionsItemSelected(item)
    }

}