package io.github.gmathi.novellibrary.domain.usecase

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.createOrUpdateLargePreference
import io.github.gmathi.novellibrary.util.Constants

class ClearRecentlyViewedNovelsUseCase(
    private val dbHelper: DBHelper
) {
    
    operator fun invoke() {
        dbHelper.createOrUpdateLargePreference(
            Constants.LargePreferenceKeys.RVN_HISTORY,
            "[]"
        )
    }
}
