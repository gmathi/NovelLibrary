package io.github.gmathi.novellibrary.util

/**
 * Constants used by multiple classes in this package
 */
object Constants {

    const val BACKUP_DIR = "NovelLibraryApp-Backup"
    const val SCROLL_LENGTH = 30
    const val FILE_PROTOCOL = "file://"



    //region DownloadQueue Status Values


    //endregion

    //region Intent Keys
    const val NOVEL_ID = "novelId"
    const val WEB_PAGE_ID = "webPageId"
    const val TOTAL_CHAPTERS_COUNT = "totalChaptersCount"
    const val CURRENT_CHAPTER_COUNT = "currentChapterCount"

    //endregion

    //region Broadcast Actions
    const val DOWNLOAD_QUEUE_NOVEL_UPDATE = "novelUpdate"
    const val DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE = "novelDownloadComplete"
    const val NOVEL_DELETED = "novelDeleted"


    //endregion

    const val IMAGES_DIR_NAME = "images"

    //region Activity Codes
    const val SEARCH_REQ_CODE = 1
    const val NOVEL_DETAILS_REQ_CODE = 2
    const val NOVEL_DETAILS_RES_CODE = 3
    const val READER_ACT_REQ_CODE = 4
    const val SEARCH_RESULTS_ACT_REQ_CODE = 5
    const val DOWNLOAD_QUEUE_ACT_RES_CODE = 6
    const val CHAPTER_ACT_REQ_CODE = 7
    const val METADATA_ACT_REQ_CODE = 8
    const val SETTINGS_ACT_REQ_CODE = 9
    const val LANG_ACT_REQ_CODE = 11
    const val IMPORT_LIBRARY_ACT_REQ_CODE = 14
    const val RECENT_UPDATED_ACT_REQ_CODE = 15
    const val RECENT_VIEWED_ACT_REQ_CODE = 16
    const val IWV_ACT_REQ_CODE = 16
    const val NOVEL_SECTIONS_ACT_REQ_CODE = 17

    const val SETTINGS_ACT_RES_CODE = 10
    const val LANG_ACT_RES_CODE = 12
    const val OPEN_DOWNLOADS_RES_CODE = 13


    //endregion

    //region Meta Data Keys

    const val MD_OTHER_LINKED_WEB_PAGES = "otherWebPages"


    //endregion

    const val NO_NETWORK = "No Network"
    const val CHAPTER_PAGE_SIZE = 15
    const val DOWNLOADING = "Downloading"

    //Intent Keys
    const val JUMP = "jumpToReader"

    interface Action {
        companion object {
            const val MAIN_ACTION = "com.truiton.foregroundservice.action.main"
            const val PREV_ACTION = "com.truiton.foregroundservice.action.prev"
            const val PLAY_ACTION = "com.truiton.foregroundservice.action.play"
            const val NEXT_ACTION = "com.truiton.foregroundservice.action.next"
            const val STARTFOREGROUND_ACTION = "com.truiton.foregroundservice.action.startforeground"
            const val STOPFOREGROUND_ACTION = "com.truiton.foregroundservice.action.stopforeground"
        }
    }

    object MetaDataKeys {
        const val LAST_READ_DATE = "lastReadDate"
        const val LAST_UPDATED_DATE = "lastUpdatedDate"
    }



}
