package io.github.gmathi.novellibrary.model.source

import android.content.Context
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.online.HttpSource
import io.github.gmathi.novellibrary.source.*


open class SourceManager(private val context: Context) {

    private val sourcesMap = mutableMapOf<Long, Source>()

    private val stubSourcesMap = mutableMapOf<Long, StubSource>()

    init {
        createInternalSources().forEach { registerSource(it) }
    }

    open fun get(sourceKey: Long): Source? {
        return sourcesMap[sourceKey]
    }

    fun getOrStub(sourceKey: Long): Source {
        return sourcesMap[sourceKey] ?: stubSourcesMap.getOrPut(sourceKey) {
            StubSource(sourceKey)
        }
    }

    fun getOnlineSources() = sourcesMap.values.filterIsInstance<HttpSource>()

    fun getCatalogueSources() = sourcesMap.values.filterIsInstance<CatalogueSource>()

    internal fun registerSource(source: Source, overwrite: Boolean = false) {
        if (overwrite || !sourcesMap.containsKey(source.id)) {
            sourcesMap[source.id] = source
        }
        if (overwrite || !stubSourcesMap.containsKey(source.id)) {
            stubSourcesMap[source.id] = StubSource(source.id)
        }
    }

    internal fun unregisterSource(source: Source) {
        sourcesMap.remove(source.id)
    }

    private fun createInternalSources(): List<Source> = listOf(
        NovelUpdatesSource(),
        WLNUpdatesSource()
    )

    private inner class StubSource(override val id: Long) : Source {

        override val name: String
            get() = id.toString()

        override suspend fun fetchNovelDetails(novel: Novel): Novel {
            throw getSourceNotInstalledException()
        }

        override suspend fun getChapterList(novel: Novel): List<WebPage> {
            throw getSourceNotInstalledException()
        }

        override fun toString(): String {
            return name
        }

        private fun getSourceNotInstalledException(): Exception {
            return Exception(context.getString(R.string.source_not_installed, id.toString()))
        }
    }
}
