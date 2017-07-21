package com.mgn.bingenovelreader.activity

import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import com.afollestad.materialdialogs.MaterialDialog
import com.mgn.bingenovelreader.R
import com.mgn.bingenovelreader.dataCenter
import com.mgn.bingenovelreader.dbHelper
import com.mgn.bingenovelreader.extension.applyFont
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_settings.*
import java.io.File


class SettingsActivity : BaseActivity() {

    lateinit var dialog: MaterialDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        dialog = MaterialDialog.Builder(this)
            .title(getString(R.string.clearing_data))
            .content(getString(R.string.please_wait))
            .progress(true, 0).cancelable(false).canceledOnTouchOutside(false).build()

        setupViews()
    }

    private fun setupViews() {
        clearCacheButton.setOnClickListener {
            dialog.show()
            deleteCache(this)
            deleteFiles(this)
            dbHelper.removeAll()
            dataCenter.saveSearchHistory(ArrayList())
            dialog.hide()
        }
        clearCacheTitle.applyFont(assets)
        clearCacheDescription.applyFont(assets)
    }

    fun deleteCache(context: Context) {
        try {
            val dir = context.cacheDir
            deleteDir(dir)
        } catch (e: Exception) {
        }

    }

    fun deleteFiles(context: Context) {
        try {
            val dir = context.filesDir
            deleteDir(dir)
        } catch (e: Exception) {
        }

    }

    fun deleteDir(dir: File?): Boolean {
        if (dir != null && dir.isDirectory) {
            val children = dir.list()
            for (i in children.indices) {
                deleteDir(File(dir, children[i]))
            }
            return dir.delete()
        } else if (dir != null && dir.isFile) {
            return dir.delete()
        } else {
            return false
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        return super.onOptionsItemSelected(item)
    }


}
