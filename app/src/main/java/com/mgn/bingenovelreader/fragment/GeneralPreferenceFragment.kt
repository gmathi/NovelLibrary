package com.mgn.bingenovelreader.fragment

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceFragment
import android.view.MenuItem
import com.mgn.bingenovelreader.R


/**
 * This fragment shows general preferences only. It is used when the
 * activity is showing a two-pane settings UI.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB)
class GeneralPreferenceFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.pref_general)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        val id = item.itemId
//        if (id == android.R.id.home) {
//            return true
//        }
        return super.onOptionsItemSelected(item)
    }
}