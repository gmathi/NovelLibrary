# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve base classes and interfaces
-keep class io.github.gmathi.novellibrary.core.activity.BaseActivity { *; }
-keep class io.github.gmathi.novellibrary.core.fragment.BaseFragment { *; }
-keep class io.github.gmathi.novellibrary.core.activity.settings.BaseSettingsActivity { *; }
-keep interface io.github.gmathi.novellibrary.core.system.DataAccessor { *; }

# Keep all public classes and methods in core module
-keep public class io.github.gmathi.novellibrary.core.** { public *; }
