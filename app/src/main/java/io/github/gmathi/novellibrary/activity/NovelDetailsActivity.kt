package io.github.gmathi.novellibrary.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.lifecycleScope
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.CloudflareResolverActivity
import io.github.gmathi.novellibrary.compose.noveldetails.NovelDetailsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.system.openInBrowser
import io.github.gmathi.novellibrary.util.system.startChaptersActivity
import io.github.gmathi.novellibrary.util.system.startMetadataActivity
import io.github.gmathi.novellibrary.util.system.startSearchResultsActivity
import io.github.gmathi.novellibrary.util.system.toast
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsEvent
import io.github.gmathi.novellibrary.viewmodel.NovelDetailsViewModel
import kotlinx.coroutines.launch

class NovelDetailsActivity : AppCompatActivity() {

    companion object {
        const val TAG = "NovelDetailsActivity"
    }

    private val viewModel: NovelDetailsViewModel by viewModels()

    private val cloudflareResolverLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.onCloudflareResolved()
        }
    }

    private val chaptersLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Constants.OPEN_DOWNLOADS_RES_CODE) {
            setResult(result.resultCode)
            finish()
            return@registerForActivityResult
        }
        // Refresh novel state in case it was added to library from the chapters screen
        val novel = intent.getParcelableExtra<Novel>("novel") as? Novel ?: return@registerForActivityResult
        viewModel.onNovelAddedFromChapters(novel.url)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val novel = intent.getParcelableExtra<Novel>("novel") as? Novel
        if (novel == null) {
            finish()
            return
        }

        viewModel.init(novel)

        lifecycleScope.launch {
            viewModel.events.collect { event ->
                handleEvent(event)
            }
        }

        setContent {
            NovelLibraryTheme {
                NovelDetailsScreen(
                    viewModel = viewModel,
                    onBackClick = { finish() },
                    onImageClick = { viewModel.onImageClick() },
                    onChaptersClick = { viewModel.onChaptersClick() },
                    onMetadataClick = { viewModel.onMetadataClick() },
                    onOpenInBrowser = { viewModel.onOpenInBrowser() },
                    onAuthorLinkClick = { title, url -> viewModel.onAuthorLinkClick(title, url) },
                    onDeleteConfirmed = { viewModel.deleteFromLibrary() },
                    onShareClick = { viewModel.onShareUrl() }
                )
            }
        }
    }

    private fun handleEvent(event: NovelDetailsEvent) {
        when (event) {
            is NovelDetailsEvent.NavigateBack -> finish()

            is NovelDetailsEvent.NavigateToChapters -> {
                startChaptersActivity(event.novel, false)
            }

            is NovelDetailsEvent.NavigateToMetadata -> startMetadataActivity(event.novel)

            is NovelDetailsEvent.NavigateToImagePreview -> {
                val intent = Intent(this, ImagePreviewActivity::class.java).apply {
                    putExtra("url", event.imageUrl)
                    putExtra("filePath", event.imageFilePath)
                }
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    this,
                    android.R.anim.fade_in,
                    android.R.anim.fade_out
                )
                startActivity(intent, options.toBundle())
            }

            is NovelDetailsEvent.NavigateToSearchResults -> startSearchResultsActivity(event.title, event.url)

            is NovelDetailsEvent.OpenInBrowser -> openInBrowser(event.url)

            is NovelDetailsEvent.ShareUrl -> shareUrl(event.url)

            is NovelDetailsEvent.ShowMessage -> toast(event.message)

            is NovelDetailsEvent.LaunchCloudflareResolver -> {
                val url = "https://${HostNames.NOVEL_UPDATES}"
                val intent = CloudflareResolverActivity.createIntent(this, url)
                cloudflareResolverLauncher.launch(intent)
            }
        }
    }

    private fun shareUrl(url: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, url)
        }
        startActivity(Intent.createChooser(intent, "Share"))
    }

    @Deprecated("Replaced by Activity Result API")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
    }
}
