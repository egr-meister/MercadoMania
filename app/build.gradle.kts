plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------
// Release signing is read from environment variables only. There is no
// fallback to the debug key: a release artifact is either signed with the real
// upload/release key or the build fails loudly (see the taskGraph check below).
// ---------------------------------------------------------------------------
val envKeystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val envKeystorePassword: String? = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val envKeyAlias: String? = System.getenv("ANDROID_KEY_ALIAS")
val envKeyPassword: String? = System.getenv("ANDROID_KEY_PASSWORD")

val hasReleaseSigning: Boolean =
    !envKeystorePath.isNullOrBlank() &&
        !envKeystorePassword.isNullOrBlank() &&
        !envKeyAlias.isNullOrBlank() &&
        !envKeyPassword.isNullOrBlank()

android {
    namespace = "com.mercadomania.game"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mercadomania.game"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(envKeystorePath!!)
                storePassword = envKeystorePassword
                keyAlias = envKeyAlias
                keyPassword = envKeyPassword
                // The keystore generated in the README is PKCS12.
                storeType = "PKCS12"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            // Staged R8: ship the first release non-minified, verify it on a
            // device, then flip both flags to true and verify again.
            // See README -> "Staged R8".
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
    }

    androidResources {
        // WAV SFX are already tiny; leaving them uncompressed lets SoundPool
        // memory-map them straight out of the APK.
        noCompress += "wav"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }

    lint {
        // Placeholder art intentionally trips a few "unused resource" style
        // checks; do not let them abort a release build.
        abortOnError = false
        checkReleaseBuilds = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose: every package imported by the app is a DIRECT dependency here,
    // including runtime, foundation and animation.
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.datastore.core)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

// ---------------------------------------------------------------------------
// Fail fast (and clearly) when a release artifact is requested without signing
// credentials, instead of silently emitting an unsigned or debug-signed build.
// ---------------------------------------------------------------------------
gradle.taskGraph.whenReady {
    val artifactPrefixes = listOf("assemble", "bundle", "package")
    val aggregateTasks = setOf("assemble", "bundle", "build")

    val wantsReleaseArtifact = allTasks.any { task ->
        if (task.project != project) return@any false
        val name = task.name
        name in aggregateTasks ||
            (name.endsWith("Release") && artifactPrefixes.any { name.startsWith(it) })
    }

    if (wantsReleaseArtifact && !hasReleaseSigning) {
        throw GradleException(
            """
            |
            |Mercado Mania: refusing to build a release artifact without release signing.
            |
            |A release APK/AAB must be signed with the real PKCS12 release keystore.
            |Falling back to the debug key is never acceptable, so the build stops here.
            |
            |Set all four environment variables and re-run:
            |  ANDROID_KEYSTORE_PATH      absolute path to the .p12/.jks keystore
            |  ANDROID_KEYSTORE_PASSWORD  keystore password
            |  ANDROID_KEY_ALIAS          key alias inside the keystore
            |  ANDROID_KEY_PASSWORD       key password
            |
            |See README.md -> "Generating a PKCS12 keystore" and "Local release signing".
            """.trimMargin()
        )
    }
}
