package io.github.gmathi.novellibrary.activity.settings

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
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
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import io.github.gmathi.novellibrary.util.applyFont
import io.github.gmathi.novellibrary.util.setDefaults
import io.github.gmathi.novellibrary.util.system.*
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
        const val CODE_NAME_WW = "code_unlock_wwd"

    }

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
        @SuppressLint("SetTextI18n")
        versionTextView.text = "Version: ${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}"
        setRemoteConfig()
        setRecyclerView()
        setEasterEgg()
    }

    @Suppress("DEPRECATION")
    private fun setRemoteConfig() {
        remoteConfig.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().build())
        val defaults = HashMap<String, Any>()
        defaults[CODE_NAME_SCRIB] = DEFAULT_CODE
        defaults[CODE_NAME_NF] = DEFAULT_CODE
        defaults[CODE_NAME_RRL] = DEFAULT_CODE
        defaults[CODE_NAME_WW] = DEFAULT_CODE
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.settings_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        recyclerView.setDefaults(adapter)
        recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        itemView.settingsTitle.applyFont(assets).text = item
        itemView.chevron.visibility = if (position < 4) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
        else ContextCompat.getColor(this, android.R.color.transparent))
    }

    override fun onItemClick(item: String, position: Int) {
        when (item) {
            getString(R.string.general) -> startGeneralSettingsActivity()
            getString(R.string.reader) -> startReaderSettingsActivity()
            getString(R.string.mentions) -> startMentionSettingsActivity()
            getString(R.string.sync) -> startSyncSettingsSelectionActivity()
            getString(R.string.donate_developer) -> donateDeveloperDialog()
            getString(R.string.about_us) -> aboutUsDialog()
            getString(R.string.cloud_flare_check) -> startCloudFlareBypassActivity("novelupdates.com")
        }
    }

    //region OptionsMenu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_settings, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) finish()
        if (item.itemId == R.id.action_report_page) {
            val systemInfo = systemInfo()
            MaterialDialog.Builder(this)
                    .content(getString(R.string.bug_report_content, "\n\n" + systemInfo))
                    .positiveText(getString(R.string.okay))
                    .neutralText(getString(R.string.copy_to_clipboard))
                    .onPositive { dialog, _ -> dialog.dismiss() }
                    .onNeutral { _, _ ->
                        val clipboard: ClipboardManager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Debug-info", systemInfo)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(this, "Debug-info copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                    .autoDismiss(false)
                    .show()
        }
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
                .title(getString(R.string.about_us))
                .content(getString(R.string.about_us_content))
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
                .input("Oops no hints!!", "I love novels a lot is a true statement!!", true) { _, input ->
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
                    CODE_NAME_RRL -> dataCenter.lockRoyalRoad = !dataCenter.lockRoyalRoad
                    CODE_NAME_NF -> dataCenter.lockNovelFull = !dataCenter.lockNovelFull
                    CODE_NAME_SCRIB -> dataCenter.lockScribble = !dataCenter.lockScribble
                    CODE_NAME_WW -> dataCenter.disableWuxiaDownloads = !dataCenter.disableWuxiaDownloads
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

    private fun systemInfo(): String {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        val builder = StringBuilder("Debug-info:")
                .append("\n\tApp Version: ").append(BuildConfig.VERSION_NAME).append('_').append(BuildConfig.VERSION_CODE)
                .append("\n\tOS Version: ").append(System.getProperty("os.version")).append('(').append(Build.VERSION.INCREMENTAL).append(')')
                .append("\n\tOS API Level: ").append(Build.VERSION.SDK_INT).append('(').append(Build.VERSION.CODENAME).append(')')
                .append("\n\tManufacturer: ").append(Build.MANUFACTURER)
                .append("\n\tDevice: ").append(Build.DEVICE)
                .append("\n\tModel (and Product): ").append(Build.MODEL).append(" (").append(Build.PRODUCT).append(')')
                .append("\n\tDisplay: ").append(displayMetrics.widthPixels).append('x').append(displayMetrics.heightPixels)
        return builder.toString()
    }

}
