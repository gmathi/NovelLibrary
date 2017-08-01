package io.github.gmathi.novellibrary.activity

import android.graphics.Color
import android.graphics.Rect
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
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size


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
        itemView.chevron.visibility = if (position == 0 || position == 1 || position == 3 || position == 4) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.general) -> startGeneralSettingsActivity()
            getString(R.string.copyright_notice) -> startCopyrightActivity()
            getString(R.string.donate_developer) -> donateDeveloperDialog()
            getString(R.string.libraries_used) -> startLibrariesUsedActivity()
            getString(R.string.contributions) -> startContributionsActivity()
            getString(R.string.changelog) -> showChangeLog()
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
            sendEmail("gmathi.developer@gmail.com", "[BUG REPORT]", "Bug Report: \n //Add Your Bug Details Below \n")
        return super.onOptionsItemSelected(item)
    }
    //endregion

    private fun donateDeveloperDialog() {
        MaterialDialog.Builder(this)
            .title(getString(R.string.donate_developer))
            //.content(getString(R.string.donate_content))
            .content(getString(R.string.donations_description_new))
            //.positiveText(R.string.donate)
//            .onPositive { _, _ ->
//                run {
//                    sendEmail(getString(R.string.dev_email), "[DONATION]", getString(R.string.donation_email_body))
//                }
//            }
            .show()
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
        if (rightButtonCounter >= 5 && leftButtonCounter >= 5) {
            dataCenter.lockRoyalRoad = false
            hiddenRightButton.visibility = View.GONE
            hiddenLeftButton.visibility = View.GONE
            viewKonfetti.build()
                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(2000L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(Size(12))
                .setPosition(-50f, viewKonfetti.width + 50f, -50f, -50f)
                .stream(300, 5000L)
        } else {
            viewKonfetti.build()
                .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                .setDirection(0.0, 359.0)
                .setSpeed(1f, 5f)
                .setFadeOutEnabled(true)
                .setTimeToLive(100L)
                .addShapes(Shape.RECT, Shape.CIRCLE)
                .addSizes(Size(12))
                .setPosition(-50f, viewKonfetti.width + 50f, -50f, -50f)
                .stream(300, 5000L)
        }
    }

    private fun showChangeLog() {
        MaterialDialog.Builder(this)
            .title("Change Log")
            .customView(R.layout.dialog_change_log, true)
            .show()
    }

}
