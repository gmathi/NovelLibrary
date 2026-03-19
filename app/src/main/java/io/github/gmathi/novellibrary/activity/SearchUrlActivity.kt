package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import io.github.gmathi.novellibrary.compose.SearchUrlScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.viewmodel.SearchUrlViewModel

class SearchUrlActivity : BaseActivity() {

    private val viewModel: SearchUrlViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val title = intent.getStringExtra("title") ?: "Search"
        val url = intent.getStringExtra("url")
        val rank = intent.getStringExtra("rank")

        setContent {
            NovelLibraryTheme {
                Surface {
                    LaunchedEffect(Unit) {
                        viewModel.initialize(rank, url)
                    }
                    
                    SearchUrlScreen(
                        viewModel = viewModel,
                        title = title,
                        onNovelClick = { novel ->
                            startNovelDetailsActivity(novel, false)
                        },
                        onBackClick = { finish() }
                    )
                }
            }
        }
    }
}
