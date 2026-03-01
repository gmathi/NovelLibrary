# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep model classes from obfuscation
-keep class io.github.gmathi.novellibrary.common.model.** { *; }

# Keep adapter classes from obfuscation
-keep class io.github.gmathi.novellibrary.common.adapter.** { *; }

# Keep UI component classes from obfuscation
-keep class io.github.gmathi.novellibrary.common.ui.** { *; }
