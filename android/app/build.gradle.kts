plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jlleitschuh.gradle.ktlint")
}

android {
    namespace = "com.wscanplus.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wscanplus.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests {
            // Allows android.util.Log calls to return defaults (0/null) in JVM unit tests
            // rather than throwing RuntimeException. A proper logging abstraction is
            // deferred to Phase 4 — this is the minimal fix to unblock JVM test execution.
            isReturnDefaultValues = true
        }
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

secrets {
    // Reads GOOGLE_MAPS_API_KEY and CROWDSEC_CTI_API_KEY from android/local.properties (gitignored).
    // Fallback placeholder values from android/secrets.defaults.properties (committed).
    // Both keys are injected into BuildConfig fields automatically by the plugin.
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase AI Logic SDK (Gemini in-app threat analysis — Android only)
    // BOM manages all firebase-* versions. Do NOT pin firebase-ai explicitly.
    // google-services plugin + google-services.json required before Firebase
    // initialises at runtime.
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")

    // Google Maps SDK — scan history heatmap + GPS-tagged scan visualisation
    // API key injected from local.properties via secrets-gradle-plugin (GOOGLE_MAPS_API_KEY)
    implementation("com.google.android.gms:play-services-maps:20.0.0")
    // Maps utility library — HeatmapTileProvider for threat-weighted GPS heatmap (#9)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // Kotlin coroutines — required by CrowdSecCtiClient (withContext) and other async flows
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // CrowdSec CTI client — /v2/smoke IP reputation lookups (Phase 3)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Keystore-backed EncryptedSharedPreferences for Kismet config and DB passphrase at rest
    implementation("androidx.security:security-crypto:1.0.0")

    // SQLCipher — AES-256 at-rest encryption for WscanDatabase (#169)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
