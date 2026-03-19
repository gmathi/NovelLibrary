package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import io.github.gmathi.novellibrary.compose.RecentNovelsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.system.startNovelDetailsActivity
import io.github.gmathi.novellibrary.viewmodel.RecentNovelsViewModel

class RecentNovelsActivity : ComponentActivity() {

    private val viewModel: RecentNovelsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            NovelLibraryTheme {
                RecentNovelsScreen(
                    viewModel = viewModel,
                    onNovelClick = { novel ->
                        startNovelDetailsActivity(novel)
                    },
                    onRecentlyUpdatedItemClick = { item ->
                        if (item.novelName != null && item.novelUrl != null) {
                            startNovelDetailsActivity(
                                Novel(item.novelName!!, item.novelUrl!!, Constants.SourceId.NOVEL_UPDATES)
                            )
                        }
                    },
                    onBackClick = { finish() }
                )
            }
        }
    }
}
