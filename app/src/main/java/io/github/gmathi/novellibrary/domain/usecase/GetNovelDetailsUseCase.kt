package io.github.gmathi.novellibrary.domain.usecase

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovelByUrl
import io.github.gmathi.novellibrary.database.updateNovel
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.util.error.Exceptions

class GetNovelDetailsUseCase(
    private val sourceManager: SourceManager,
    private val dbHelper: DBHelper
) {
    suspend operator fun invoke(novel: Novel): Result<Novel> {
        return try {
            val source = sourceManager.get(novel.sourceId)
                ?: return Result.failure(Exception(Exceptions.MISSING_SOURCE_ID))
            val updatedNovel = source.getNovelDetails(novel)
            if (updatedNovel.id != -1L) {
                dbHelper.updateNovel(updatedNovel)
            }
            Result.success(updatedNovel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
