package io.github.gmathi.novellibrary.util

import android.view.View.*
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.network.HostNames

/**
 * Constants used by multiple classes in this package
 */
object Constants {
    const val SCROLL_LENGTH = 30
    const val FILE_PROTOCOL = "file://"
    const val SYSTEM_DEFAULT = "systemDefault"
    const val VOLUME_SCROLL_LENGTH_DEFAULT = 2
    const val VOLUME_SCROLL_LENGTH_STEP = 250
    const val VOLUME_SCROLL_LENGTH_MIN = -10
    const val VOLUME_SCROLL_LENGTH_MAX = 10
    const val IMMERSIVE_MODE_FLAGS = (SYSTEM_UI_FLAG_LAYOUT_STABLE
            or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or SYSTEM_UI_FLAG_FULLSCREEN
            or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    const val IMMERSIVE_MODE_W_NAVBAR_FLAGS = (SYSTEM_UI_FLAG_LAYOUT_STABLE
            or SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            or SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            or SYSTEM_UI_FLAG_FULLSCREEN
            or SYSTEM_UI_FLAG_IMMERSIVE_STICKY)

    const val DEFAULT_FONT_PATH = "/android_asset/fonts/source_sans_pro_regular.ttf"

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
    const val SETTINGS_ACT_RES_CODE = 10
    const val LANG_ACT_REQ_CODE = 11
    const val LANG_ACT_RES_CODE = 12
    const val OPEN_DOWNLOADS_RES_CODE = 13
    const val IMPORT_LIBRARY_ACT_REQ_CODE = 14
    const val RECENT_UPDATED_ACT_REQ_CODE = 15
    const val IWV_ACT_REQ_CODE = 16
    const val NOVEL_SECTIONS_ACT_REQ_CODE = 17
    const val OPEN_FIREBASE_AUTH_UI = 18
    const val RECENT_VIEWED_ACT_REQ_CODE = 19
    const val READER_SETTINGS_ACT_REQ_CODE = 20
    const val READER_BACKGROUND_SETTINGS_ACT_REQ_CODE = 20

    const val TTS_ACT_REQ_CODE = 1000
    const val ADD_FONT_REQUEST_CODE = 1101


    //endregion


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
        const val SHOW_SOURCES = "showSources"
        const val SCROLL_POSITION = "scrollY"
        const val OTHER_LINKED_WEB_PAGES = "otherWebPages"
        const val OTHER_LINKED_WEB_PAGES_SETTINGS = "otherWebPagesSettings"
        const val IS_FAVORITE = "isFavorite"
    }

    object LargePreferenceKeys {
        const val RVN_HISTORY = "recentlyViewNovelsHistory"
    }

    const val SIMPLE_NOVEL_BACKUP_FILE_NAME = "SimpleNovelBackup.txt"
    const val DATABASES_DIR = "databases"
    const val FILES_DIR = "files"
    const val SHARED_PREFS_DIR = "shared_prefs"
    const val DATA_SUBFOLDER = """/data/${BuildConfig.APPLICATION_ID}"""

    const val WORK_KEY_RESULT = "result"

    const val WLN_UPDATES_API_URL = "https://www.${HostNames.WLN_UPDATES}/api"
    const val NEOVEL_API_URL = "https://${HostNames.NEOVEL}/"
    const val LNMTL_BASE_URL = "https://${HostNames.LNMTL}/"

    const val ALL_TRANSLATOR_SOURCES = "ALL"

    object Status {
        const val NO_INTERNET = "noInternet"
        const val NETWORK_ERROR = "networkError"
        const val EMPTY_DATA = "emptyData"
        const val START = "Loading…"
        const val DONE = "done"
    }

    object RemoteConfig {
        const val SELECTOR_QUERIES = "selector_queries"
    }

    object SourceId {
        const val NOVEL_UPDATES = 1L
        const val WLN_UPDATES = 2L
        const val NEOVEL = 3L
        const val LNMTL = 4L
        const val ROYAL_ROAD = 5L
        const val SCRIBBLE_HUB = 6L
        const val NOVEL_FULL = 7L

    }

}
