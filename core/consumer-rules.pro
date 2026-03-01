# Consumer ProGuard rules for core module
# These rules are automatically applied to consumers of this library

# Preserve base classes and interfaces for consumers
-keep class io.github.gmathi.novellibrary.core.activity.BaseActivity { *; }
-keep class io.github.gmathi.novellibrary.core.fragment.BaseFragment { *; }
-keep class io.github.gmathi.novellibrary.core.activity.settings.BaseSettingsActivity { *; }
-keep interface io.github.gmathi.novellibrary.core.system.DataAccessor { *; }
