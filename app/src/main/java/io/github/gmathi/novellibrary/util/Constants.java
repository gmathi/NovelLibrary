/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.gmathi.novellibrary.util;

/**
 * Constants used by multiple classes in this package
 */
public class Constants {

    public static final String BACKUP_DIR = "NovelLibraryApp-Backup";
    public static final int SCROLL_LENGTH = 30;

    public interface ACTION {
        public static String MAIN_ACTION = "com.truiton.foregroundservice.action.main";
        public static String PREV_ACTION = "com.truiton.foregroundservice.action.prev";
        public static String PLAY_ACTION = "com.truiton.foregroundservice.action.play";
        public static String NEXT_ACTION = "com.truiton.foregroundservice.action.next";
        public static String STARTFOREGROUND_ACTION = "com.truiton.foregroundservice.action.startforeground";
        public static String STOPFOREGROUND_ACTION = "com.truiton.foregroundservice.action.stopforeground";
    }

    public interface NOTIFICATION_ID {
        public static int FOREGROUND_SERVICE = 101;
        public static int FOREGROUND_DOWNLOAD_NOVEL_SERVICE = 102;
        public static int SYNC_CHAPTERS = 103;
    }


    //region DownloadQueue Status Values
    public static final Long STATUS_DOWNLOAD = 0L;
    public static final Long STATUS_STOPPED = 1L;
    public static final Long STATUS_COMPLETE = 2L;

    //endregion

    //region Intent Keys
    public static final String NOVEL_ID = "novelId";
    public static final String WEB_PAGE_ID = "webPageId";
    public static final String TOTAL_CHAPTERS_COUNT = "totalChaptersCount";
    public static final String CURRENT_CHAPTER_COUNT = "currentChapterCount";

    //endregion

    //region Broadcast Actions
    public static final String DOWNLOAD_QUEUE_NOVEL_UPDATE = "novelUpdate";
    public static final String DOWNLOAD_QUEUE_NOVEL_DOWNLOAD_COMPLETE = "novelDownloadComplete";
    public static final String NOVEL_DELETED = "novelDeleted";


    //endregion

    public static final String IMAGES_DIR_NAME = "images";

    //region Activity Codes
    public static final int SEARCH_REQ_CODE = 1;
    public static final int NOVEL_DETAILS_REQ_CODE = 2;
    public static final int NOVEL_DETAILS_RES_CODE = 3;
    public static final int READER_ACT_REQ_CODE = 4;
    public static final int SEARCH_RESULTS_ACT_REQ_CODE = 5;
    public static final int DOWNLOAD_QUEUE_ACT_RES_CODE = 6;
    public static final int CHAPTER_ACT_REQ_CODE = 7;
    public static final int METADATA_ACT_REQ_CODE = 8;
    public static final int SETTINGS_ACT_REQ_CODE = 9;
    public static final int LANG_ACT_REQ_CODE = 11;
    public static final int IMPORT_LIBRARY_ACT_REQ_CODE = 14;

    public static final int SETTINGS_ACT_RES_CODE = 10;
    public static final int LANG_ACT_RES_CODE = 12;
    public static final int OPEN_DOWNLOADS_RES_CODE = 13;



    //endregion

    //region Meta Data Keys

    public static final String MD_OTHER_LINKED_WEB_PAGES = "otherWebPages";


    //endregion

    public static final String NO_NETWORK = "No Network";
    public static final Integer CHAPTER_PAGE_SIZE = 15;
    public static final String DOWNLOADING = "Downloading";

    //Intent Keys
    public static final String JUMP = "jumpToReader";





}
