package io.github.gmathi.novellibrary.activity.settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import io.github.gmathi.novellibrary.activity.BaseActivity

/**
 * Minimal standalone test screen for AI TTS using Jetpack Compose.
 *
 * espeak-ng-data MUST live on the filesystem (native code requirement).
 * The ONNX model + tokens are loaded via AssetManager.
 */
class AiTtsTestActivity : ComponentActivity() {

    private val viewModel: AiTtsTestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        viewModel.initEngine(filesDir, assets)

        setContent {
            MaterialTheme {
                Surface {
                    AiTtsTestScreen(
                        viewModel = viewModel,
                        onNavigateBack = { finish() }
                    )
                }
            }
        }
    }
}
