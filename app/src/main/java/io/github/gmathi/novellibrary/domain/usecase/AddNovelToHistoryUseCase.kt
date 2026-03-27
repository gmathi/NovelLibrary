package io.github.gmathi.novellibrary.domain.usecase

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.createOrUpdateLargePreference
import io.github.gmathi.novellibrary.database.deleteLargePreference
import io.github.gmathi.novellibrary.database.getLargePreference
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.util.Constants

class AddNovelToHistoryUseCase(
    private val dbHelper: DBHelper,
    private val gson: Gson
) {
    operator fun invoke(novel: Novel) {
        try {
            var history = dbHelper.getLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY) ?: "[]"
            var historyList: ArrayList<Novel> = gson.fromJson(history, object : TypeToken<ArrayList<Novel>>() {}.type)
            historyList.removeAll { novel.name == it.name }
            if (historyList.size > 99) historyList = ArrayList(historyList.take(99))
            historyList.add(novel)
            history = gson.toJson(historyList)
            dbHelper.createOrUpdateLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY, history)
        } catch (e: Exception) {
            dbHelper.deleteLargePreference(Constants.LargePreferenceKeys.RVN_HISTORY)
        }
    }
}
