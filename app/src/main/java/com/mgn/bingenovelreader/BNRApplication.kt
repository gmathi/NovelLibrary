package com.mgn.bingenovelreader

import android.app.Application
import com.mgn.bookmark.util.DataCenter


val dataCenter: DataCenter by lazy {
    BNRApplication.dataCenter!!
}

class BNRApplication : Application() {
    companion object {
        var dataCenter: DataCenter? = null
    }

    override fun onCreate() {
        dataCenter = DataCenter(applicationContext)
        super.onCreate()
    }
}