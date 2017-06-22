package com.mgn.bingenovelreader.activities

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity


abstract class BaseActivity : AppCompatActivity() {

    lateinit var broadcastReceiver: BroadcastReceiver

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        super.onCreate(savedInstanceState, persistentState)

        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    manageBroadcasts(intent)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        unregisterReceiver(broadcastReceiver)
        super.onPause()
    }

    fun registerReceiver() {
        val filter = IntentFilter()
        getBroadcastIntentActions().forEach { filter.addAction(it) }
        filter.addCategory(Intent.CATEGORY_DEFAULT)
        registerReceiver(broadcastReceiver, filter)
    }

    abstract fun getBroadcastIntentActions(): ArrayList<String>
    abstract fun manageBroadcasts(intent: Intent)



}