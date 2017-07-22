package io.github.gmathi.novellibrary.activity

import android.os.Bundle
import android.os.PersistableBundle
import android.support.v7.app.AppCompatActivity


abstract class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?, persistentState: PersistableBundle?) {
        //setTheme(if (dataCenter.isDarkTheme) R.style.DarkTheme_DarkSide else R.style.DarkTheme)
        super.onCreate(savedInstanceState, persistentState)
    }


}