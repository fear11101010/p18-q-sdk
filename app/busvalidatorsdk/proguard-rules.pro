# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

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


# Keep all public classes and methods for Android (don't obfuscate)
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class com.dtca.busvalidator.busvalidatorsdk.model.** {*;}
-keep public class com.dtca.busvalidator.busvalidatorsdk.helper.** {*;}
-keep public class com.dtca.busvalidator.busvalidatorsdk.db.** {*;}
#-keep public class com.dtca.busvalidator.busvalidatorsdk.model.TransactionData
#-keep public class com.dtca.busvalidator.busvalidatorsdk.model.Route
#-keep public class com.dtca.busvalidator.busvalidatorsdk.model.FareMatrix
#-keep public class com.dtca.busvalidator.busvalidatorsdk.Sam
#-keep public class com.dtca.busvalidator.busvalidatorsdk.FelicaCard
#-keep public class com.dtca.busvalidator.busvalidatorsdk.helper.Utils

-keep class * {
    static <fields>;
    static <methods>;
}

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep classes used by reflection
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
    @com.google.gson.annotations.SerializedName <methods>;
}

# Log class (optional)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# Preserve all annotations
-keepattributes Signature
-keepattributes *Annotation*
