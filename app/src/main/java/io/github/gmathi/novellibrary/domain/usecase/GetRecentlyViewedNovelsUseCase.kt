package io.github.gmathi.novellibrary.domain.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getLargePreference
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs

class GetRecentlyViewedNovelsUseCase(
    private val dbHelper: DBHelper,
    private val gson: Gson
) {
    
    operator fun invoke(): List<Novel> {
        val history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY) ?: "[]"
        val historyList: ArrayList<Novel> = gson.fromJson(
            history,
            object : TypeToken<ArrayList<Novel>>() {}.type
        )
        Logs.debug("GetRecentlyViewedNovelsUseCase", "Found ${historyList.size} recently viewed novels\n$historyList")
        return historyList.asReversed()
    }
}
