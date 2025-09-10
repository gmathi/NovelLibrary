package io.github.gmathi.novellibrary.fragment

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.MenuItem
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.TEXT_ALIGNMENT_CENTER
import android.view.WindowManager
import android.webkit.MimeTypeMap
import android.webkit.WebView
import android.widget.CompoundButton
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.navArgs
import androidx.viewpager.widget.ViewPager
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.list.checkItem
import com.afollestad.materialdialogs.list.listItemsSingleChoice
import com.google.android.material.navigation.NavigationView
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.reader.ReaderSettingsActivity
import io.github.gmathi.novellibrary.adapter.GenericFragmentStatePagerAdapter
import io.github.gmathi.novellibrary.adapter.WebPageFragmentPageListener
import io.github.gmathi.novellibrary.database.getWebPageSettingsByRedirectedUrl
import io.github.gmathi.novellibrary.database.getWebPageSettings
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.database.updateWebPageSettings
import io.github.gmathi.novellibrary.databinding.FragmentReaderMainBinding
import io.github.gmathi.novellibrary.model.ReaderUiState
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.other.ReaderSettingsEvent
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Constants.VOLUME_SCROLL_LENGTH_STEP
import io.github.gmathi.novellibrary.util.FAC
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.Utils.getFormattedText
import io.github.gmathi.novellibrary.util.system.intentOf
import io.github.gmathi.novellibrary.util.system.logNovelEvent
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.util.system.showAlertDialog
import io.github.gmathi.novellibrary.util.system.startTTSActivity
import io.github.gmathi.novellibrary.util.system.startTTSService
import io.github.gmathi.novellibrary.util.view.TwoWaySeekBar
import io.github.gmathi.novellibrary.viewmodel.ReaderViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File
import java.util.Random

/**
 * Fragment for reading novels with WebView-based content display and reader controls
 */
@AndroidEntryPoint
class ReaderFragment : BaseViewBindingFragment<FragmentReaderMainBinding>(),
    ViewPager.OnPageChangeListener, NavigationView.OnNavigationItemSelectedListener {

    companion object {
        private const val TAG = "ReaderFragment"

        private val FONT_MIME_TYPES = arrayOf(
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("ttf") ?: "application/x-font-ttf",
            "fonts/ttf",
            MimeTypeMap.getSingleton().getMimeTypeFromExtension("otf") ?: "application/x-font-opentype",
            "fonts/otf",
            "application/octet-stream"
        )

        private val AVAILABLE_FONTS = linkedMapOf<String, String>()
    }

    private val args: ReaderFragmentArgs by navArgs()
    private val viewModel: ReaderViewModel by viewModels()

    private lateinit var novel: Novel
    private lateinit var adapter: GenericFragmentStatePagerAdapter
    private var webPages: List<WebPage> = ArrayList()
    private var translatorSourceName: String? = null

    override fun getLayoutId() = R.layout.fragment_reader_main

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): android.view.View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        if (view != null) {
            setBinding(FragmentReaderMainBinding.bind(view))
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupUI()
        observeViewModel()
        initializeReader()
    }

    private fun setupUI() {
        // Keep screen on if enabled
        if (dataCenter.keepScreenOn) {
            requireActivity().window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Setup drawer navigation
        drawerSetup()
        binding.menuNav.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.END)
        }

        // Easy access to open reader menu button
        if (!dataCenter.isReaderModeButtonVisible) {
            binding.menuNav.visibility = INVISIBLE
        }

        // Handle back press
        requireActivity().onBackPressedDispatcher.addCallback(
            viewLifecycleOwner,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val currentFrag = binding.viewPager.adapter?.instantiateItem(
                        binding.viewPager,
                        binding.viewPager.currentItem
                    ) as? WebPageDBFragment
                    
                    when {
                        currentFrag?.history?.isNotEmpty() == true -> currentFrag.goBack()
                        else -> {
                            isEnabled = false
                            requireActivity().onBackPressedDispatcher.onBackPressed()
                        }
                    }
                }
            }
        )

        // Setup volume key navigation if enabled
        if (viewModel.isVolumeScrollEnabled()) {
            setupVolumeKeyNavigation()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ReaderUiState.Loading -> {
                    // Show loading state if needed
                }
                is ReaderUiState.Success -> {
                    showContent(state)
                }
                is ReaderUiState.Error -> {
                    showError(state.message)
                }
            }
        }
    }

    private fun initializeReader() {
        // Get novel ID and chapter ID from arguments
        val novelId = args.novelId
        val chapterId = args.chapterId
        translatorSourceName = args.translatorSourceName
        
        // Initialize the reader with novel and chapter IDs
        viewModel.initializeReader(novelId, chapterId, translatorSourceName)
    }

    private fun showContent(state: ReaderUiState.Success) {
        novel = state.novel
        webPages = state.webPages
        
        // Setup ViewPager with fragments
        adapter = GenericFragmentStatePagerAdapter(
            childFragmentManager,
            null,
            webPages.size,
            WebPageFragmentPageListener(novel, webPages)
        )
        
        binding.viewPager.addOnPageChangeListener(this)
        binding.viewPager.adapter = adapter
        
        // Set current page
        binding.viewPager.currentItem = state.currentPageIndex
        
        // Update bookmark for the current page if it's the first page
        if (state.currentPageIndex == 0) {
            viewModel.updateBookmark(webPages[0])
        }
    }

    private fun showError(message: String) {
        Logs.error(TAG, "Error in reader: $message")
        // Handle error display - could show a toast or error view
    }

    @Suppress("DEPRECATION")
    override fun onResume() {
        super.onResume()
        
        // Update last read time
        viewModel.updateLastRead()
        
        // Handle immersive mode
        if (dataCenter.enableImmersiveMode) {
            binding.mainContent.fitsSystemWindows = false
            requireActivity().window.decorView.systemUiVisibility = Constants.IMMERSIVE_MODE_FLAGS
        }
    }

    override fun onPageSelected(position: Int) {
        if (webPages.isNotEmpty() && position < webPages.size) {
            val currentChapter = webPages[position]
            
            // Update bookmark in ViewModel
            viewModel.updateBookmark(currentChapter)
            
            // Update reading progress
            updateReadingProgress(currentChapter, position)
            
            // Mark chapter as read automatically (can be made configurable later)
            markChapterAsRead(currentChapter)
        }
    }

    /**
     * Update reading progress for the current chapter
     */
    private fun updateReadingProgress(chapter: WebPage, position: Int) {
        try {
            // Calculate reading progress percentage
            val totalChapters = webPages.size
            val progressPercentage = if (totalChapters > 0) {
                ((position + 1).toFloat() / totalChapters.toFloat()) * 100f
            } else {
                0f
            }
            
            // Update novel reading progress in database
            novel.let { currentNovel ->
                currentNovel.currentChapterUrl = chapter.url
                
                // Update in database if novel is in library
                if (currentNovel.id != -1L) {
                    lifecycleScope.launch {
                        try {
                            dbHelper.updateNovel(currentNovel)
                        } catch (e: Exception) {
                            Logs.error(TAG, "Error updating novel", e)
                        }
                    }
                }
            }
            
            Logs.debug(TAG, "Updated reading progress: ${progressPercentage}% for chapter: ${chapter.chapterName}")
            
        } catch (e: Exception) {
            Logs.error(TAG, "Error updating reading progress", e)
        }
    }

    /**
     * Mark chapter as read
     */
    private fun markChapterAsRead(chapter: WebPage) {
        try {
            lifecycleScope.launch {
                try {
                    val webPageSettings = dbHelper.getWebPageSettings(chapter.url)
                    if (webPageSettings != null && !webPageSettings.isRead) {
                        webPageSettings.isRead = true
                        dbHelper.updateWebPageSettings(webPageSettings)
                        Logs.debug(TAG, "Marked chapter as read: ${chapter.chapterName}")
                    }
                } catch (e: Exception) {
                    Logs.error(TAG, "Error updating web page settings", e)
                }
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Error marking chapter as read", e)
        }
    }

    override fun onPageScrollStateChanged(position: Int) {
        // Do Nothing
    }

    override fun onPageScrolled(p0: Int, p1: Float, p2: Int) {
        // Do Nothing
    }

    private fun changeTextSize() {
        MaterialDialog(requireContext()).show {
            title(R.string.text_size)
            customView(R.layout.dialog_slider, scrollable = true)
            getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar)?.setOnSeekBarChangedListener { _, progress ->
                dataCenter.textSize = progress.toInt()
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.TEXT_SIZE))
            }
            getCustomView().findViewById<TwoWaySeekBar>(R.id.seekBar)?.setProgress(dataCenter.textSize.toDouble())
        }
    }

    private fun inBrowser() {
        val url = (binding.viewPager.adapter?.instantiateItem(
            binding.viewPager,
            binding.viewPager.currentItem
        ) as WebPageDBFragment?)?.getUrl()
        if (url != null) {
            requireActivity().openInBrowser(url)
        }
    }

    fun checkUrl(url: String): Boolean {
        val webPageSettings = dbHelper.getWebPageSettingsByRedirectedUrl(url) ?: return false
        return viewModel.navigateToPage(url)
    }

    /**
     * Navigate to the next chapter with smooth scrolling
     */
    fun navigateToNextChapter(): Boolean {
        val currentIndex = binding.viewPager.currentItem
        return if (currentIndex < webPages.size - 1) {
            binding.viewPager.setCurrentItem(currentIndex + 1, true) // true for smooth scrolling
            true
        } else {
            false
        }
    }

    /**
     * Navigate to the previous chapter with smooth scrolling
     */
    fun navigateToPreviousChapter(): Boolean {
        val currentIndex = binding.viewPager.currentItem
        return if (currentIndex > 0) {
            binding.viewPager.setCurrentItem(currentIndex - 1, true) // true for smooth scrolling
            true
        } else {
            false
        }
    }

    /**
     * Navigate to a specific chapter by index with smooth scrolling
     */
    fun navigateToChapter(chapterIndex: Int, smoothScroll: Boolean = true): Boolean {
        return if (chapterIndex in 0 until webPages.size) {
            binding.viewPager.setCurrentItem(chapterIndex, smoothScroll)
            true
        } else {
            false
        }
    }

    /**
     * Navigate to a specific chapter by WebPage with smooth scrolling
     */
    fun navigateToChapter(webPage: WebPage, smoothScroll: Boolean = true): Boolean {
        val index = webPages.indexOfFirst { it.url == webPage.url }
        return if (index != -1) {
            binding.viewPager.setCurrentItem(index, smoothScroll)
            true
        } else {
            false
        }
    }

    /**
     * Get the current chapter index
     */
    fun getCurrentChapterIndex(): Int = binding.viewPager.currentItem

    /**
     * Get the current chapter
     */
    fun getCurrentChapter(): WebPage? {
        val currentIndex = binding.viewPager.currentItem
        return if (currentIndex in 0 until webPages.size) {
            webPages[currentIndex]
        } else {
            null
        }
    }

    /**
     * Setup volume key navigation for chapter pagination
     */
    private fun setupVolumeKeyNavigation() {
        binding.root.isFocusableInTouchMode = true
        binding.root.requestFocus()
        
        binding.root.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && viewModel.isVolumeScrollEnabled()) {
                when (keyCode) {
                    KeyEvent.KEYCODE_VOLUME_UP -> {
                        // Volume up = previous chapter (standard behavior)
                        navigateToPreviousChapter()
                        true
                    }
                    KeyEvent.KEYCODE_VOLUME_DOWN -> {
                        // Volume down = next chapter (standard behavior)
                        navigateToNextChapter()
                        true
                    }
                    else -> false
                }
            } else {
                false
            }
        }
    }

    private fun drawerSetup() {
        binding.drawerLayout.closeDrawers()
        binding.navigationView.apply {
            setNavigationItemSelectedListener(this@ReaderFragment)

            val readerSwitch = menu.findItem(R.id.title_reader).actionView as CompoundButton
            readerSwitch.isChecked = dataCenter.readerMode
            readerSwitch.setOnCheckedChangeListener { _, isChecked ->
                dataCenter.readerMode = isChecked
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.READER_MODE))
            }

            val jsSwitch = menu.findItem(R.id.title_java_script).actionView as CompoundButton
            jsSwitch.isChecked = !dataCenter.javascriptDisabled
            jsSwitch.setOnCheckedChangeListener { _, isChecked ->
                dataCenter.javascriptDisabled = !isChecked
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.JAVA_SCRIPT))
            }
        }
    }

    private fun createTypeface(path: String = dataCenter.fontPath): Typeface {
        return if (path.startsWith("/android_asset")) {
            Typeface.createFromAsset(requireContext().assets, path.substringAfter('/').substringAfter('/'))
        } else {
            Typeface.createFromFile(path)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.title_fonts -> changeFontStyle()
            R.id.title_fonts_size -> changeTextSize()
            R.id.title_open_in_browser -> inBrowser()
            R.id.title_more_settings -> readerSettingsActivityContract.launch(requireContext().intentOf<ReaderSettingsActivity>())
            R.id.title_read_aloud -> {
                if (dataCenter.readerMode) {
                    val webPageDBFragment = (binding.viewPager.adapter?.instantiateItem(
                        binding.viewPager,
                        binding.viewPager.currentItem
                    ) as? WebPageDBFragment)
                    val audioText = webPageDBFragment?.doc?.getFormattedText(dataCenter) ?: return true
                    val title = webPageDBFragment.doc?.title() ?: ""
                    val chapterIndex = (if (dataCenter.japSwipe) webPages.reversed() else webPages)
                        .indexOf(webPages[binding.viewPager.currentItem])

                    (requireActivity() as AppCompatActivity).startTTSService(audioText, webPageDBFragment.linkedPages, title, novel.id, translatorSourceName, chapterIndex)
                    firebaseAnalytics.logNovelEvent(FAC.Event.LISTEN_NOVEL, novel)
                    (requireActivity() as AppCompatActivity).startTTSActivity()
                } else {
                    showAlertDialog(title = "Read Aloud", message = "Only supported in Reader Mode!")
                }
            }
        }
        return true
    }

    @SuppressLint("CheckResult")
    private fun changeFontStyle() {
        if (AVAILABLE_FONTS.isEmpty()) {
            getAvailableFonts()
        }

        var selectedFont = dataCenter.fontPath.substringAfterLast('/')
            .substringBeforeLast('.')
            .replace('_', ' ')

        var typeFace = createTypeface()

        MaterialDialog(requireContext()).show {
            title(R.string.title_fonts)

            val exampleText = TextView(requireContext())
            exampleText.textAlignment = TEXT_ALIGNMENT_CENTER
            exampleText.text = getString(R.string.title_fonts)
            exampleText.textSize = 24F
            exampleText.setTypeface(typeFace, Typeface.NORMAL)
            customView(view = exampleText)

            listItemsSingleChoice(
                items = AVAILABLE_FONTS.keys.toMutableList(),
                waitForPositiveButton = false
            ) { dialog, which, font ->
                if (which == 0) {
                    addFont()
                    dialog.dismiss()
                } else {
                    Logs.debug(TAG, "font $which $font")
                    val fontPath = AVAILABLE_FONTS[font.toString()]
                    if (fontPath != null) {
                        selectedFont = font.toString()
                        typeFace = createTypeface(fontPath)

                        val customView = getCustomView() as TextView
                        customView.setTypeface(typeFace, Typeface.NORMAL)

                        setActionButtonEnabled(WhichButton.POSITIVE, true)
                    } else {
                        dialog.checkItem(AVAILABLE_FONTS.keys.indexOf(selectedFont))
                    }
                }
            }
            positiveButton(R.string.okay) { _ ->
                dataCenter.fontPath = AVAILABLE_FONTS[selectedFont] ?: ""
                EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
            }
            negativeButton(R.string.cancel)
        }
    }

    @Synchronized
    private fun getAvailableFonts() {
        if (AVAILABLE_FONTS.isNotEmpty()) return

        AVAILABLE_FONTS[getString(R.string.add_font)] = ""

        requireContext().assets.list("fonts")?.filter {
            it.endsWith(".ttf") || it.endsWith(".otf")
        }?.forEach {
            AVAILABLE_FONTS[it.substringBeforeLast('.').replace('_', ' ')] = "/android_asset/fonts/$it"
        }

        val appFontsDir = File(requireContext().getExternalFilesDir(null) ?: requireContext().filesDir, "Fonts")
        if (!appFontsDir.exists()) appFontsDir.mkdir()
        appFontsDir.listFiles()?.forEach {
            AVAILABLE_FONTS[it.nameWithoutExtension.replace('_', ' ')] = it.path
        }
    }

    private fun addFont() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            .addCategory(Intent.CATEGORY_OPENABLE)
            .setType("*/*")
            .putExtra(Intent.EXTRA_MIME_TYPES, FONT_MIME_TYPES)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            .addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        addFontActivityContract.launch(intent)
    }

    private val addFontActivityContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        try {
            if (it.resultCode != Activity.RESULT_OK) return@registerForActivityResult
            val uri = it.data?.data ?: return@registerForActivityResult
            val document = DocumentFile.fromSingleUri(requireContext(), uri) ?: return@registerForActivityResult
            if (!document.isFile) return@registerForActivityResult

            val fontsDir = File(requireContext().getExternalFilesDir(null) ?: requireContext().filesDir, "Fonts/")
            if (!fontsDir.exists()) fontsDir.mkdir()
            val file = File(fontsDir, document.name ?: "RandomFontName${Random().nextInt()}")
            Utils.copyFile(requireContext().contentResolver, document, file)
            AVAILABLE_FONTS[file.nameWithoutExtension.replace('_', ' ')] = file.path
            dataCenter.fontPath = file.path
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.FONT))
        } catch (e: Exception) {
            Logs.error(TAG, "Unable to copy font", e)
        }
    }

    private val readerSettingsActivityContract = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Handler(Looper.getMainLooper()).post {
            EventBus.getDefault().post(ReaderSettingsEvent(ReaderSettingsEvent.NIGHT_MODE))
        }
    }
}