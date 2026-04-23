package io.github.gmathi.novellibrary.compose.library

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme

class SortBottomSheetDialogFragment : BottomSheetDialogFragment() {

    var onSortSelected: ((SortOption) -> Unit)? = null

    companion object {
        const val TAG = "SortBottomSheetDialog"

        fun newInstance(): SortBottomSheetDialogFragment {
            return SortBottomSheetDialogFragment()
        }
    }

    override fun getTheme(): Int {
        return R.style.Theme_NovelLibrary_BottomSheet
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.skipCollapsed = true
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                NovelLibraryTheme {
                    SortBottomSheetContent(
                        onSortSelected = { option ->
                            onSortSelected?.invoke(option)
                            dismiss()
                        },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)
                    )
                }
            }
        }
    }
}
