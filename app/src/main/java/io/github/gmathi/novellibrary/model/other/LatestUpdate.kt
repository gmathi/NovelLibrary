package io.github.gmathi.novellibrary.model.other

data class LatestUpdate(val versionCode: Int, val versionName: String, val apk: String, var hasUpdate: Boolean = false)
