package io.github.gmathi.novellibrary.util

//Firebase Analytics Constants
object FAC {

    object Event {
        const val SEARCH_NOVEL = "search_novel"
        const val ADD_NOVEL = "add_novel"
        const val REMOVE_NOVEL = "remove_novel"
        const val OPEN_NOVEL = "open_novel"
        const val LISTEN_NOVEL = "listen_novel"
        const val DOWNLOAD_NOVEL = "download_novel"
        const val READ_NOVEL = "read_novel"


        const val ADD_NOVEL_SECTION = "add_novel_section"
        const val REMOVE_NOVEL_SECTION = "remove_novel_section"
        const val RENAME_NOVEL_SECTION = "rename_novel_section"

        const val SELECTOR_QUERY = "selector_query"
    }

    object Param {
        const val NOVEL_NAME = "novel_name"
        const val NOVEL_URL = "novel_url"

        const val NOVEL_SECTION_NAME = "novel_section_name"

        const val CHAPTER_NAME = "chapter_name"
        const val CHAPTER_URL = "chapter_url"
        const val NOVEL_IMAGE_URL = "novel_image_url"
        const val CHAPTER_IS_DOWNLOADED = "chapter_is_downloaded"

        const val SEARCH_TERM = "search_term"

    }
}

