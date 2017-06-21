package com.mgn.bingenovelreader

import android.app.Application
import com.mgn.bingenovelreader.database.DBHelper
import com.mgn.bookmark.util.DataCenter
import java.io.File


val dataCenter: DataCenter by lazy {
    BNRApplication.dataCenter!!
}

val dbHelper: DBHelper by lazy {
    BNRApplication.dbHelper!!
}

class BNRApplication : Application() {
    companion object {
        var dataCenter: DataCenter? = null
        var dbHelper: DBHelper? = null
    }

    override fun onCreate() {
        dataCenter = DataCenter(applicationContext)
        dbHelper = DBHelper(applicationContext)
        super.onCreate()

        val imagesDir = File(filesDir, "images")
        if (!imagesDir.exists())
            imagesDir.mkdir()
    }
}