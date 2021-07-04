package io.github.gmathi.novellibrary.extension.model

data class ExtensionItem(val extension: Extension, val header: ExtensionGroupItem? = null, val installStep: InstallStep? = null)