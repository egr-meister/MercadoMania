import java.io.File
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

// ---------------------------------------------------------------------------
// White/Black integration secrets. They live in local.properties (never
// committed) or, on CI, in environment variables. Empty is a valid value: the
// integration degrades gracefully - a blank Adjust token or offer URL simply
// keeps the app on the White (game) side.
// ---------------------------------------------------------------------------
val integrationProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun secret(name: String, default: String = ""): String =
    System.getenv(name) ?: integrationProperties.getProperty(name) ?: default

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

// ---------------------------------------------------------------------------
// google-services.json materialisation (Firebase Cloud Messaging).
//
// The file never enters the repository. It arrives like the other secrets:
// GOOGLE_SERVICES_JSON (base64) from local.properties locally, GitHub Secrets on
// CI. The google-services plugin needs a file per build type whose package_name
// matches the applied applicationId, so one is written for debug and one for
// release before the plugin reads them.
//
//   - debug ALWAYS gets a stub: its applicationId ends in `.debug`, which the real
//     production file never matches, and the plugin fails on a mismatch.
//   - release gets the real decoded file when the secret is present, otherwise a
//     stub so a local build still assembles. A stub ships WITHOUT working push:
//     project_number is all zeros. CI rejects a release without the real secret
//     (see .github/workflows); locally this is only good for White/Black, routing,
//     signals and signature - not for verifying push.
// ---------------------------------------------------------------------------
val gameApplicationId = "com.mercadomania.game"

fun stubGoogleServices(packageName: String): String = """
    {
      "project_info": {
        "project_number": "000000000000",
        "project_id": "mercado-mania-stub",
        "storage_bucket": "mercado-mania-stub.appspot.com"
      },
      "client": [
        {
          "client_info": {
            "mobilesdk_app_id": "1:000000000000:android:0000000000000000000000",
            "android_client_info": { "package_name": "$packageName" }
          },
          "oauth_client": [],
          "api_key": [ { "current_key": "AIzaSyStubStubStubStubStubStubStubStub0000" } ],
          "services": { "appinvite_service": { "other_platform_oauth_client": [] } }
        }
      ],
      "configuration_version": "1"
    }
""".trimIndent()

fun writeGoogleServices(buildTypeDir: String, packageName: String, allowReal: Boolean) {
    val outDir = File(projectDir, "src/$buildTypeDir")
    outDir.mkdirs()
    val out = File(outDir, "google-services.json")
    val base64 = secret("GOOGLE_SERVICES_JSON")
    val real = if (allowReal && base64.isNotBlank()) {
        runCatching { String(Base64.getMimeDecoder().decode(base64.trim())) }
            .getOrNull()
            ?.takeIf { it.contains("\"project_number\"") }
    } else {
        null
    }
    if (real != null) {
        out.writeText(real)
    } else {
        if (allowReal) {
            logger.warn("GOOGLE_SERVICES_JSON is absent or unparsable - writing a push-less stub for $buildTypeDir.")
        }
        out.writeText(stubGoogleServices(packageName))
    }
}

// Written at configuration time so both files exist before the google-services
// plugin processes them.
writeGoogleServices(buildTypeDir = "debug", packageName = "$gameApplicationId.debug", allowReal = false)
writeGoogleServices(buildTypeDir = "release", packageName = gameApplicationId, allowReal = true)

android {
    namespace = "com.mercadomania.game"
    // Stays at 36 (Android 16, stable): API 37 ships only as the android-37.0 preview
    // platform, and the sibling apps from this skill ship at 36 too. Bump when android-37
    // is a stable platform.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.mercadomania.game"
        minSdk = 24
        targetSdk = 36
        // Bumped for the Adjust + FCM migration, the localisation / theme pass, and the
        // gps_adid (GAID) key in the single click.
        versionCode = 5
        versionName = "1.0.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // No resourceConfigurations filter: all seven shipped locales
        // (values + values-ru/fr/es/pt/it/de) must survive into the APK.

        // buildConfigField values are pasted into generated Java verbatim, so the
        // escaped quotes are part of the string. Adjust attribution + Keitaro offer
        // routing; empty means White-only.
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"${secret("ADJUST_APP_TOKEN")}\"")
        buildConfigField("String", "OFFER_BASE_URL", "\"${secret("OFFER_BASE_URL")}\"")
        // aso | prily - the campaign group decides the URL shape (sub1 in path for prily).
        buildConfigField("String", "OFFER_ROUTING", "\"${secret("OFFER_ROUTING", "aso")}\"")
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
    // AppCompat: MainActivity is an AppCompatActivity so the in-app language picker
    // (AppCompatDelegate.setApplicationLocales) survives a restart below API 33.
    implementation(libs.androidx.appcompat)

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

    // White/Black integration: Adjust attribution + Firebase Cloud Messaging.
    implementation(libs.adjust)
    // installreferrer and the AdID provider are declared explicitly: without Play Install
    // Referrer and GAID, Adjust attribution degrades to organic and the tracker gets
    // traffic with no campaign and no sub1..subN.
    implementation(libs.installreferrer)
    implementation(libs.play.services.ads.identifier)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
    // Restores the X-Requested-With header the WebView drops since M108.
    implementation(libs.androidx.webkit)

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
