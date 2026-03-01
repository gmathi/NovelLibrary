# Consumer ProGuard rules for common module
# These rules are automatically applied to modules that depend on this module

# Keep model classes from obfuscation
-keep class io.github.gmathi.novellibrary.common.model.** { *; }

# Keep adapter classes and their interfaces from obfuscation
-keep class io.github.gmathi.novellibrary.common.adapter.** { *; }

# Keep UI component classes from obfuscation
-keep class io.github.gmathi.novellibrary.common.ui.** { *; }
