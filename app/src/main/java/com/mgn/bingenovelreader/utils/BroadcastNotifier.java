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

package com.mgn.bingenovelreader.utils;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

public class BroadcastNotifier {

    private LocalBroadcastManager mBroadcaster;

    public BroadcastNotifier(Context context) {
        mBroadcaster = LocalBroadcastManager.getInstance(context);
    }

    public void broadcastNovelUpdate(long novelId) {
        Intent localIntent = new Intent();
        localIntent.setAction(Constants.DOWNLOAD_QUEUE_NOVEL_UPDATE);
        localIntent.putExtra(Constants.NOVEL_ID, novelId);
        localIntent.addCategory(Intent.CATEGORY_DEFAULT);
        mBroadcaster.sendBroadcast(localIntent);
    }

//    public void notifyProgress(String logData) {
//
//        Intent localIntent = new Intent();
//
//        // The Intent contains the custom broadcast action for this app
//        localIntent.setAction(Constants.BROADCAST_ACTION);
//
//        localIntent.putExtra(Constants.EXTENDED_DATA_STATUS, -1);
//
//        // Puts log data into the Intent
//        localIntent.putExtra(Constants.EXTENDED_STATUS_LOG, logData);
//        localIntent.addCategory(Intent.CATEGORY_DEFAULT);
//
//        // Broadcasts the Intent
//        mBroadcaster.sendBroadcast(localIntent);
//
//    }
}
