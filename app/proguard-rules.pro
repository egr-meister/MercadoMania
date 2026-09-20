# ---------------------------------------------------------------------------
# Mercado Mania - R8 / ProGuard rules
#
# The app currently ships with isMinifyEnabled = false. These rules are kept
# ready so that enabling R8 (see README -> "Staged R8") does not break
# Kotlinx Serialization, which resolves serializers reflectively at runtime.
# ---------------------------------------------------------------------------

# Keep line numbers useful in crash reports.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# --- Kotlinx Serialization --------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Keep the generated serializer objects and the `Companion.serializer()` entry
# points for every @Serializable class in the app.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    *** Companion;
}
-keepclasseswithmembers class ** {
    @kotlinx.serialization.Serializable <fields>;
}

# The serialization runtime itself.
-keep,includedescriptorclasses class kotlinx.serialization.** { *; }
-dontnote kotlinx.serialization.**
-dontwarn kotlinx.serialization.**

# Our own serializable models and navigation routes.
-keep class com.mercadomania.game.data.** { *; }
-keep class com.mercadomania.game.ui.nav.** { *; }

# --- Navigation Compose type-safe routes ------------------------------------
# Routes are @Serializable objects/data classes resolved by reflection.
-keepnames class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**

# --- Compose ----------------------------------------------------------------
-dontwarn androidx.compose.**

# --- Coroutines -------------------------------------------------------------
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
