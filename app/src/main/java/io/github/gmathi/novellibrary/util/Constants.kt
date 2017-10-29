package io.github.gmathi.novellibrary.util

object Constants {

    val BACKUP_DIR = "NovelLibraryApp-Backup"
    val SCROLL_LENGTH = 30

    object ACTION {
        val MAIN_ACTION = "com.truiton.foregroundservice.action.main"
        val PREV_ACTION = "com.truiton.foregroundservice.action.prev"
        val PLAY_ACTION = "com.truiton.foregroundservice.action.play"
        val NEXT_ACTION = "com.truiton.foregroundservice.action.next"
        val STARTFOREGROUND_ACTION = "com.truiton.foregroundservice.action.startforeground"
        val STOPFOREGROUND_ACTION = "com.truiton.foregroundservice.action.stopforeground"

    }

    object NOTIFICATION_ID {
        val FOREGROUND_SERVICE = 101
        val FOREGROUND_DOWNLOAD_NOVEL_SERVICE = 102
        val SYNC_CHAPTERS = 103
    }

    //region DownloadQueue Status Values
    val STATUS_DOWNLOAD = 0L
    val STATUS_STOPPED = 1L
    val STATUS_COMPLETE = 2L

    //endregion

    //region Intent Keys
    val NOVEL_ID = "novelId"
    val WEB_PAGE_ID = "webPageId"
    val TOTAL_CHAPTERS_COUNT = "totalChaptersCount"
    val CURRENT_CHAPTER_COUNT = "currentChapterCount"

    //endregion

    //region Broadcast Actions
    val DOWNLOAD_QUEUE_NOVEL_UPDATE = "novelUpdate"
    val DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE = "novelDownloadComplete"
    val NOVEL_DELETED = "novelDeleted"


    //endregion

    val IMAGES_DIR_NAME = "images"

    //region Activity Codes
    val SEARCH_REQ_CODE = 1
    val NOVEL_DETAILS_REQ_CODE = 2
    val NOVEL_DETAILS_RES_CODE = 3
    val READER_ACT_REQ_CODE = 4
    val SEARCH_RESULTS_ACT_REQ_CODE = 5
    val DOWNLOAD_QUEUE_ACT_RES_CODE = 6
    val CHAPTER_ACT_REQ_CODE = 7
    val METADATA_ACT_REQ_CODE = 8
    val SETTINGS_ACT_REQ_CODE = 9
    val LANG_ACT_REQ_CODE = 11
    val IMPORT_LIBRARY_ACT_REQ_CODE = 14
    val RECENT_UPDATED_ACT_REQ_CODE = 15
    val RECENT_VIEWED_ACT_REQ_CODE = 16

    val SETTINGS_ACT_RES_CODE = 10
    val LANG_ACT_RES_CODE = 12
    val OPEN_DOWNLOADS_RES_CODE = 13


    //endregion

    //region Meta Data Keys

    val MD_OTHER_LINKED_WEB_PAGES = "otherWebPages"


    //endregion

    val NO_NETWORK = "No Network"
    val CHAPTER_PAGE_SIZE = 15
    val DOWNLOADING = "Downloading"

    //Intent Keys
    val JUMP = "jumpToReader"
}