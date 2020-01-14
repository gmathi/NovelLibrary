package io.github.gmathi.novellibrary.activity.settings

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import com.afollestad.materialdialogs.MaterialDialog
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.extensions.startGeneralSettingsActivity
import io.github.gmathi.novellibrary.extensions.startMentionSettingsActivity
import io.github.gmathi.novellibrary.extensions.startReaderSettingsActivity
import io.github.gmathi.novellibrary.util.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import kotlinx.android.synthetic.main.activity_settings.*
import kotlinx.android.synthetic.main.content_recycler_view.*
import kotlinx.android.synthetic.main.listitem_settings.view.*
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size


class SettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        const val TAG = "SettingsActivity"
        const val DEFAULT_CODE = "defaultCode"

        const val CODE_NAME_SCRIB = "code_unlock_scrib"
        const val CODE_NAME_NF = "code_unlock_nf"
        const val CODE_NAME_RRL = "code_unlock_rrl"

    }

    override val titleRes: Int? = R.string.title_activity_settings

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>

    private var rightButtonCounter = 0
    private var leftButtonCounter = 0

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        setRemoteConfig()
        setRecyclerView()
        setEasterEgg()
    }

    private fun setRemoteConfig() {
        remoteConfig.setConfigSettings(FirebaseRemoteConfigSettings.Builder().setDeveloperModeEnabled(BuildConfig.DEBUG).build())
        val defaults = HashMap<String, Any>()
        defaults[CODE_NAME_SCRIB] = DEFAULT_CODE
        defaults[CODE_NAME_NF] = DEFAULT_CODE
        defaults[CODE_NAME_RRL] = DEFAULT_CODE
        remoteConfig.setDefaults(defaults)
        remoteConfig.fetchAndActivate()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(io.github.gmathi.novellibrary.R.array.settings_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item
        itemView.chevron.visibility = if (position == 0 || position == 1 || position == 2) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String) {
        when (item) {
            getString(R.string.general) -> startGeneralSettingsActivity()
            getString(R.string.reader) -> startReaderSettingsActivity()
            getString(R.string.mentions) -> startMentionSettingsActivity()
            getString(R.string.donate_developer) -> donateDeveloperDialog()
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
            MaterialDialog.Builder(this)
                    .content("Please use discord to report a bug.")
                    .positiveText("Ok")
                    .onPositive { dialog, _ -> dialog.dismiss() }
                    .show()
        //            sendEmail("gmathi.developer@gmail.com", "[BUG REPORT]", "Bug Report: \n //Add Your Bug Details Below \n")
        return super.onOptionsItemSelected(item)
    }
    //endregion

    private fun donateDeveloperDialog() {
        MaterialDialog.Builder(this)
                .title(getString(R.string.donate_developer))
                .content(getString(R.string.donations_description_new))
                .show()
    }

    private fun aboutUsDialog() {
        MaterialDialog.Builder(this)
                .title("Version: ${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}")
                .content(getString(R.string.lock_hint))
                .show()
    }

    private fun setEasterEgg() {
        hiddenRightButton.setOnClickListener { rightButtonCounter++; checkUnlockStatus() }
        hiddenLeftButton.setOnClickListener { leftButtonCounter++; checkUnlockStatus() }
    }

    private fun checkUnlockStatus() {
        if (rightButtonCounter >= 5 && leftButtonCounter >= 5) {
            hiddenRightButton.visibility = View.GONE
            hiddenLeftButton.visibility = View.GONE
            showCodeDialog()
        }
    }

    private fun showCodeDialog() {
        MaterialDialog.Builder(this)
                .input("Opps no hints!!", "ilovenovelsalot", true) { dialog, input ->
                    checkCode(input.toString())
                }.title("Enter Unlock Code")
                .canceledOnTouchOutside(false)
                .show()
    }

    private fun checkCode(code: String) {
        Log.e(TAG, code)
        remoteConfig.all.forEach {
            val value = it.value.asString()
            if (value == code) {
                showConfetti()
                when (it.key) {
                    CODE_NAME_RRL -> dataCenter.lockRoyalRoad = false
                    CODE_NAME_NF -> dataCenter.lockNovelFull = false
                    CODE_NAME_SCRIB -> dataCenter.lockScribble = false
                }
            }
        }
    }

    private fun showConfetti() {
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
    }
}
