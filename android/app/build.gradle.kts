plugins {
    id("com.android.application")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("org.jlleitschuh.gradle.ktlint")
}

val googleServicesFile = file("google-services.json")
val requestedTasks = gradle.startParameter.taskNames.map { it.lowercase() }
val allowMissingGoogleServices =
    requestedTasks.isEmpty() ||
        requestedTasks.all { task ->
            task.contains("test") ||
                task.contains("androidtest") ||
                task.contains("unittest") ||
                task.contains("lint") ||
                task.contains("ktlint") ||
                task.contains("jacoco") ||
                task.contains("check")
        }

if (googleServicesFile.exists()) {
    apply(plugin = "com.google.gms.google-services")
} else if (allowMissingGoogleServices) {
    logger.warn(
        "google-services.json is missing; skipping com.google.gms.google-services. " +
            "This is allowed for IDE sync/test-only tasks, but app build tasks will fail. " +
            "Requested tasks: ${if (requestedTasks.isEmpty()) "<none>" else requestedTasks.joinToString()}",
    )
} else {
    throw org.gradle.api.GradleException(
        "Missing google-services.json for app build tasks. " +
            "Firebase resources (such as google_app_id) will not be generated, which can cause runtime failures. " +
            "Add android/app/google-services.json or run only test/sync tasks. " +
            "Requested tasks: ${requestedTasks.joinToString()}",
    )
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
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")

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
