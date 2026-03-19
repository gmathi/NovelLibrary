package io.github.gmathi.novellibrary.domain.usecase

import io.github.gmathi.novellibrary.model.other.RecentlyUpdatedItem
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import io.github.gmathi.novellibrary.util.Logs

class GetRecentlyUpdatedNovelsUseCase {
    
    suspend operator fun invoke(): Result<List<RecentlyUpdatedItem>> {
        return try {
            val items = mutableListOf<RecentlyUpdatedItem>()
            val document = WebPageDocumentFetcher.document("https://www.novelupdates.com/")
            // Try multiple selectors as the page structure may vary
            var elements = document.body().select("table#myTable tbody tr")
            
            // If the old selector doesn't work, try finding any table with the expected structure
            if (elements.isEmpty()) {
                Logs.debug("GetRecentlyUpdatedNovelsUseCase", "Table#myTable not found, trying generic table selector")
                elements = document.body().select("tbody tr")
            }
            
            Logs.debug("GetRecentlyUpdatedNovelsUseCase", "Found ${elements.size} table rows")
            
            // Log first few rows for debugging
            if (elements.isNotEmpty()) {
                Logs.debug("GetRecentlyUpdatedNovelsUseCase", "First row HTML: ${elements.first()?.outerHtml() ?: "null"}")
            }
            
            elements.forEachIndexed { index, element ->
                try {
                    val cell0 = element.selectFirst("a[href]") ?: throw Exception("No link found in cell 0")
                    val title = cell0.attr("title") ?: throw Exception("No title found in cell 0")
                    val href = cell0.attr("abs:href") ?: throw Exception("No link found in cell 0")
                    val chapter = element.selectFirst("span[title]")?.attr("title")
                    val publisher = element.select("a[title]")?.getOrNull(1)?.attr("title")
                    val item = RecentlyUpdatedItem(
                        href, title, chapter, publisher
                    )
                    items.add(item)
                } catch (e: Exception) {
                    Logs.error("GetRecentlyUpdatedNovelsUseCase", "Error parsing row $index", e)
                }
            }
            
            Logs.debug("GetRecentlyUpdatedNovelsUseCase", "Successfully parsed ${items.size} items")
            
            if (items.isEmpty()) {
                Result.failure(Exception("No recently updated novels found. The page structure may have changed."))
            } else {
                Result.success(items)
            }
        } catch (e: Exception) {
            Logs.error("GetRecentlyUpdatedNovelsUseCase", "Failed to fetch recently updated novels", e)
            Result.failure(e)
        }
    }
}
