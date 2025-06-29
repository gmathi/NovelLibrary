# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep attributes for debugging and crash reporting
-keepattributes SourceFile,LineNumberTable,Signature,Exceptions
-keepattributes *Annotation*

# Keep public classes and methods
-keep public class * extends java.lang.Exception
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.preference.Preference
-keep public class * extends android.view.View
-keep public class * extends android.app.Fragment

# Keep model classes
-keep class io.github.gmathi.novellibrary.model.** { *; }
-keepclassmembers class io.github.gmathi.novellibrary.model.** {
    <init>(...);
}

# Keep database classes
-keep class io.github.gmathi.novellibrary.database.** { *; }
-keepclassmembers class io.github.gmathi.novellibrary.database.** {
    <init>(...);
}

# Keep source classes
-keep class io.github.gmathi.novellibrary.source.** { *; }
-keepclassmembers class io.github.gmathi.novellibrary.source.** {
    <init>(...);
}

# Keep extension classes
-keep class io.github.gmathi.novellibrary.extension.** { *; }
-keepclassmembers class io.github.gmathi.novellibrary.extension.** {
    <init>(...);
}

# Glide rules for proguard
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.resource.bitmap.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}

# for DexGuard only
-keepresourcexmlelements manifest/application/meta-data@value=GlideModule

# Coil rules
-keep class coil.** { *; }
-keep interface coil.** { *; }

# OkHttp rules
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Retrofit rules
-keepattributes Signature
-keepattributes Exceptions
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepattributes *Annotation*
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# RxJava rules
-dontwarn rx.**
-keep class rx.** { *; }
-keep interface rx.** { *; }

# Firebase rules
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# Keep Firebase Crashlytics
-keep class com.google.firebase.crashlytics.** { *; }
-dontwarn com.google.firebase.crashlytics.**

# Keep Gson
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class io.github.gmathi.novellibrary.**$$serializer { *; }
-keepclassmembers class io.github.gmathi.novellibrary.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.gmathi.novellibrary.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep WorkManager
-keep class androidx.work.impl.** { *; }

# Keep Injekt dependency injection
-keep class uy.kohesive.injekt.** { *; }
-keep interface uy.kohesive.injekt.** { *; }

# Keep Jsoup
-keep class org.jsoup.** { *; }

# Keep Lottie
-keep class com.airbnb.lottie.** { *; }

# Keep Material Dialogs
-keep class com.afollestad.materialdialogs.** { *; }

# Keep SnackProgressBar
-keep class com.github.tingyik90.snackprogressbar.** { *; }

# Keep Konfetti
-keep class nl.dionsegijn.konfetti.** { *; }

# Keep PhotoView
-keep class com.github.chrisbanes.photoview.** { *; }

# Keep FancyButtons
-keep class com.github.medyo.fancybuttons.** { *; }

# Keep SmartTabLayout
-keep class com.ogaclejapan.smarttablayout.** { *; }

# Keep RecyclerView Animators
-keep class jp.wasabeef.recyclerview.** { *; }

# Keep Crux
-keep class com.github.chimbori.crux.** { *; }

# Keep Markdown
-keep class org.jetbrains.markdown.** { *; }

# Optimization rules
-optimizations !code/simplification/arithmetic,!code/simplification/cast,!field/*,!class/merging/*
-optimizationpasses 5
-allowaccessmodification
-dontpreverify

# Remove logging in release
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep enum values
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

