# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep settings activities
-keep class io.github.gmathi.novellibrary.settings.activity.** { *; }

# Keep public API
-keep class io.github.gmathi.novellibrary.settings.api.** { *; }
