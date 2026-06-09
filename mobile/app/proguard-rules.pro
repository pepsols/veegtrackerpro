# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\Nathan\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.kts.

# Keep Room classes
-keep class androidx.room.** { *; }

# Keep Moshi classes
-keep class com.squareup.moshi.** { *; }
-keepclassmembers class * {
    @com.squareup.moshi.Json *;
}

# Keep Firebase classes
-keep class com.google.firebase.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Keep Netty classes
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Keep osmdroid
-keep class org.osmdroid.** { *; }

# Other common libraries
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.eclipse.jetty.npn.**
-dontwarn reactor.blockhound.**
-dontwarn java.lang.management.**
-dontwarn com.sun.nio.file.**
