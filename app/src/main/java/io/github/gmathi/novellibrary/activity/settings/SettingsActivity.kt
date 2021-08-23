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
import com.afollestad.materialdialogs.input.input
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.BaseActivity
import io.github.gmathi.novellibrary.adapter.GenericAdapter
import io.github.gmathi.novellibrary.databinding.ActivitySettingsBinding
import io.github.gmathi.novellibrary.databinding.ListitemSettingsBinding
import io.github.gmathi.novellibrary.util.view.extensions.applyFont
import io.github.gmathi.novellibrary.util.view.setDefaults
import io.github.gmathi.novellibrary.util.system.*
import io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size


class SettingsActivity : BaseActivity(), GenericAdapter.Listener<String> {

    companion object {
        const val TAG = "SettingsActivity"
        const val DEFAULT_CODE = "defaultCode"

        const val CODE_NAME_WW = "code_unlock_wwd"

    }

    lateinit var adapter: GenericAdapter<String>
    private lateinit var settingsItems: ArrayList<String>

    private var rightButtonCounter = 0
    private var leftButtonCounter = 0

    private val remoteConfig = FirebaseRemoteConfig.getInstance()

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        @SuppressLint("SetTextI18n")
        binding.versionTextView.text = "Version: ${BuildConfig.VERSION_NAME}_${BuildConfig.VERSION_CODE}"
        setRemoteConfig()
        setRecyclerView()
        setEasterEgg()
    }

    @Suppress("DEPRECATION")
    private fun setRemoteConfig() {
        remoteConfig.setConfigSettingsAsync(FirebaseRemoteConfigSettings.Builder().build())
        val defaults = HashMap<String, Any>()
        defaults[CODE_NAME_WW] = DEFAULT_CODE
        remoteConfig.setDefaultsAsync(defaults)
        remoteConfig.fetchAndActivate()
    }

    private fun setRecyclerView() {
        settingsItems = ArrayList(resources.getStringArray(R.array.settings_list).asList())
        adapter = GenericAdapter(items = settingsItems, layoutResId = R.layout.listitem_settings, listener = this)
        binding.contentRecyclerView.recyclerView.setDefaults(adapter)
        binding.contentRecyclerView.recyclerView.addItemDecoration(CustomDividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        binding.contentRecyclerView.swipeRefreshLayout.isEnabled = false
    }

    override fun bind(item: String, itemView: View, position: Int) {
        val itemBinding = ListitemSettingsBinding.bind(itemView)
        itemBinding.settingsTitle.applyFont(assets).text = item
        itemBinding.chevron.visibility = if (position < 4) View.VISIBLE else View.INVISIBLE
        itemView.setBackgroundColor(
            if (position % 2 == 0) ContextCompat.getColor(this, R.color.black_transparent)
            else ContextCompat.getColor(this, android.R.color.transparent)
        )
    }

    override fun onItemClick(item: String, position: Int) {
        when (item) {
            getString(R.string.general) -> startGeneralSettingsActivity()
            getString(R.string.reader) -> startReaderSettingsActivity()
            getString(R.string.mentions) -> startMentionSettingsActivity()
            getString(R.string.sync) -> startSyncSettingsSelectionActivity() //underConstructionDialog("NovelSync is under a rewrite and will be back in future releases!")
            getString(R.string.donate_developer) -> donateDeveloperDialog()
            getString(R.string.about_us) -> aboutUsDialog()
            //getString(R.string.cloud_flare_check) -> underConstructionDialog()//startCloudFlareBypassActivity("novelupdates.com")
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
            MaterialDialog(this).show {
                message(R.string.bug_report_content, "\n\n" + systemInfo)
                positiveButton(R.string.okay) {
                    it.dismiss()
                }
                negativeButton(R.string.copy_to_clipboard) {
                    val clipboard: ClipboardManager =
                        getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("Debug-info", systemInfo)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@SettingsActivity, "Debug-info copied to clipboard!", Toast.LENGTH_SHORT)
                        .show()
                }
                cancelable(false)
            }
        }
        return super.onOptionsItemSelected(item)
    }
    //endregion

    private fun donateDeveloperDialog() {
        MaterialDialog(this).show {
            title(R.string.donate_developer)
            message(R.string.donations_description_new)
        }
    }

    private fun aboutUsDialog() {
        MaterialDialog(this).show {
            title(R.string.about_us)
            message(R.string.about_us_content)
        }
    }

    private fun underConstructionDialog(text: String = "Under Re-construction!") {
        MaterialDialog(this).show {
            //title(R.string.about_us)
            message(text = text)
        }
    }


    private fun setEasterEgg() {
        binding.hiddenRightButton.setOnClickListener { rightButtonCounter++; checkUnlockStatus() }
        binding.hiddenLeftButton.setOnClickListener { leftButtonCounter++; checkUnlockStatus() }
    }

    private fun checkUnlockStatus() {
        if (rightButtonCounter >= 5 && leftButtonCounter >= 5) {
            binding.hiddenRightButton.visibility = View.GONE
            binding.hiddenLeftButton.visibility = View.GONE
            showCodeDialog()
        }
    }

    @SuppressLint("CheckResult")
    private fun showCodeDialog() {
        MaterialDialog(this).show {
            title(text = "Enter Unlock Code")
            input(hint = "Oops no hints!!", prefill = "I love novels a lot is a true statement!!") { dialog, input ->
                checkCode(input.toString())
            }
            cancelOnTouchOutside(false)
        }
    }

    private fun checkCode(code: String) {
        Log.e(TAG, code)
        remoteConfig.all.forEach {
            val value = it.value.asString()
            if (value == code) {
                showConfetti()
                when (it.key) {
                    CODE_NAME_WW -> dataCenter.disableWuxiaDownloads = !dataCenter.disableWuxiaDownloads
                }
                return
            }
        }
    }

    private fun showConfetti() {
        binding.viewKonfetti.build()
            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
            .setDirection(0.0, 359.0)
            .setSpeed(1f, 5f)
            .setFadeOutEnabled(true)
            .setTimeToLive(2000L)
            .addShapes(Shape.RECT, Shape.CIRCLE)
            .addSizes(Size(12))
            .setPosition(-50f, binding.viewKonfetti.width + 50f, -50f, -50f)
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
