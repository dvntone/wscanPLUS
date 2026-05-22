plugins {
    id("com.android.application")
    id("com.google.gms.google-services") apply false
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

// Conditionally apply google-services plugin if google-services.json is present.
// This allows builds to succeed in CI and clean environments where Firebase is not configured.
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
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
    // Injects CROWDSEC_CTI_API_KEY from local.properties into BuildConfig.
    // Fallback placeholder values from android/secrets.defaults.properties (committed).
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // Firebase AI Logic SDK (Gemini in-app threat analysis — Android only)
    // BOM manages all firebase-* versions. Do NOT pin firebase-ai explicitly.
    // google-services plugin + google-services.json required before Firebase
    // initialises at runtime.
    implementation(platform("com.google.firebase:firebase-bom:34.11.0"))
    implementation("com.google.firebase:firebase-ai")

    // Kotlin coroutines — required by CrowdSecCtiClient (withContext) and other async flows
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")

    // CrowdSec CTI client — /v2/smoke IP reputation lookups (Phase 3)
    implementation("com.squareup.okhttp3:okhttp:5.3.2")

    // Keystore-backed EncryptedSharedPreferences for Kismet config and DB passphrase at rest
    implementation("androidx.security:security-crypto:1.0.0")

    // SQLCipher — AES-256 at-rest encryption for WscanDatabase (#169)
    implementation("net.zetetic:android-database-sqlcipher:4.5.4")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    testImplementation("com.squareup.okhttp3:mockwebserver:5.3.2")
    testImplementation("org.json:json:20240303")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
