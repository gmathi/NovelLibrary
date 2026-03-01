# Consumer ProGuard rules for util module
# These rules are automatically applied to consumers of this library

# Keep all public utility classes and methods
-keep public class io.github.gmathi.novellibrary.util.** { public *; }

# Keep Kotlin extension functions
-keep class io.github.gmathi.novellibrary.util.Extensions** { *; }
-keep class io.github.gmathi.novellibrary.util.view.extensions.** { *; }
