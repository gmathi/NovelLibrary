package io.github.gmathi.novellibrary.activity

import android.content.Intent
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.Menu
import android.view.MenuItem
import android.view.View
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.extension.*
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import java.io.File


class SettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    lateinit var adapter: GenericAdapter<String>
    lateinit var settingsItems: ArrayList<String>

    var rightButtonCounter = 0
    var leftButtonCounter = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRecyclerView()

        setEasterEgg()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(io.github.gmathi.novellibrary.R.array.settings_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(object : DividerItemDecoration(this, DividerItemDecoration.VERTICAL) {

            override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
                val position = parent?.getChildAdapterPosition(view)
                if (position == parent?.adapter?.itemCount?.minus(1)) {
                    outRect?.setEmpty()
                } else {
                    super.getItemOffsets(outRect, view, parent, state)
                }
            }
        })
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item
        itemView.settingsChevron.visibility = if (position == 1 || position == 3 || position == 4) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.clear_data) -> deleteFilesDialog()
            getString(R.string.copyright_notice) -> startCopyrightActivity()
            getString(R.string.donate_developer) -> donateDeveloperDialog()
            getString(R.string.libraries_used) -> startLibrariesUsedActivity()
            getString(R.string.contributions) -> startContributionsActivity()
            getString(R.string.about_us) -> aboutUsDialog()
        }
    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == android.R.id.home) finish()
        if (item?.itemId == R.id.action_report_page)
            emailTo("gmathi.developer@gmail.com", "[BUG REPORT]", "Bug Report: \n //Add Your Bug Details Below \n", "Email Bug with…")
        return super.onOptionsItemSelected(item)
    }
    //endregion

    //region Delete Files
    fun deleteFilesDialog() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.clear_data))
            .content(getString(R.string.clear_data_description))
            .positiveText(R.string.clear)
            .negativeText(R.string.cancel)
            .onPositive { dialog, _ ->
                run {
                    val progressDialog = MaterialDialog.Builder(this)
                        .title(getString(R.string.clearing_data))
                        .content(getString(R.string.please_wait))
                        .progress(true, 0)
                        .cancelable(false)
                        .canceledOnTouchOutside(false)
                        .show()
                    deleteFiles(progressDialog)
                    dialog.dismiss()
                }
            }
            .onNegative { dialog, _ ->
                run {
                    dialog.dismiss()
                }
            }
            .show()
    }

    fun deleteFiles(dialog: MaterialDialog) {
        try {
            deleteDir(cacheDir)
            deleteDir(filesDir)
            dbHelper.removeAll()
            dataCenter.saveSearchHistory(ArrayList())
            dialog.dismiss()
        } catch (e: Exception) {
            e.printStackTrace()
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
    //endregion

    private fun donateDeveloperDialog() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.donate_developer))
            .content(getString(R.string.donate_content))
            .positiveText(R.string.donate)
            .onPositive { _, _ ->
                run {
                    emailTo(getString(R.string.dev_email), "[DONATION]", getString(R.string.donation_email_body), "Select GMail…")
                }
            }.show()
    }

    private fun emailTo(email: String, subject: String, body: String, intentChooseTitle: String) {
        val uri = Uri.parse("mailto:$email")
            .buildUpon()
            .appendQueryParameter("subject", subject)
            .appendQueryParameter("body", body)
            .build()

        val emailIntent = Intent(Intent.ACTION_SENDTO, uri)
        startActivity(Intent.createChooser(emailIntent, intentChooseTitle))
    }

    private fun aboutUsDialog() {
        MaterialDialog.Builder(this)
            .title("Version: ${BuildConfig.VERSION_NAME}")
            .content(getString(R.string.lock_hint))
            .show()
    }

    private fun setEasterEgg() {
        if (dataCenter.lockRoyalRoad) {
            hiddenRightButton.setOnClickListener { rightButtonCounter++; checkUnlockStatus() }
            hiddenLeftButton.setOnClickListener { leftButtonCounter++; checkUnlockStatus() }
        }
    }

    private fun checkUnlockStatus() {
        if (rightButtonCounter >=5 && leftButtonCounter >= 5) {
            dataCenter.lockRoyalRoad = false
            hiddenRightButton.visibility = View.GONE
            hiddenLeftButton.visibility = View.GONE
        }
    }

}
