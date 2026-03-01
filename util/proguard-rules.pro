# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep utility classes and their public methods
-keep public class io.github.gmathi.novellibrary.util.** { public *; }

# Keep Kotlin extensions
-keep class io.github.gmathi.novellibrary.util.Extensions** { *; }
-keep class io.github.gmathi.novellibrary.util.view.extensions.** { *; }

# Keep LocaleManager
-keep class io.github.gmathi.novellibrary.util.system.LocaleManager { *; }

# Keep ProgressLayout
-keep class io.github.gmathi.novellibrary.util.view.ProgressLayout { *; }

# Keep CustomDividerItemDecoration
-keep class io.github.gmathi.novellibrary.util.view.CustomDividerItemDecoration { *; }
