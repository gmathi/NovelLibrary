package io.github.gmathi.novellibrary.model.source

import androidx.preference.PreferenceScreen

interface ConfigurableSource {

    fun setupPreferenceScreen(screen: PreferenceScreen)

}
