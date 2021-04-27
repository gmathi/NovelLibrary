package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import android.view.MenuItem
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.databinding.ActivityCopyrightBinding
import io.github.gmathi.novellibrary.util.view.extensions.applyFont

class CopyrightActivity : BaseActivity() {
    
    private lateinit var binding: ActivityCopyrightBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        binding = ActivityCopyrightBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.contentCopyright.copyrightContentTextView.applyFont(assets)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }

}
