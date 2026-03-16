plugins {
    id("com.android.application")
    // TODO: add id("com.google.gms.google-services") once google-services.json
    // is placed at android/app/google-services.json (obtain from Firebase Console).
    // Applying without the file will fail the build — see KNOWN_ISSUES.md.
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    namespace = "com.wscanplus.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.wscanplus.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
}

secrets {
    // Reads MAPS_API_KEY and GEMINI_API_KEY from android/local.properties (gitignored).
    // Fallback placeholder values from android/secrets.defaults.properties (committed).
    propertiesFileName = "local.properties"
    defaultPropertiesFileName = "secrets.defaults.properties"
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.core:core-ktx:1.18.0")

    // Firebase AI Logic SDK (Gemini in-app threat analysis — Android only)
    // BOM manages all firebase-* versions. Do NOT pin firebase-ai explicitly.
    // google-services plugin + google-services.json required before Firebase
    // initialises at runtime — see KNOWN_ISSUES.md.
    implementation(platform("com.google.firebase:firebase-bom:34.10.0"))
    implementation("com.google.firebase:firebase-ai")

    // Google Maps SDK — scan history heatmap + GPS-tagged scan visualisation
    // API key injected from local.properties via secrets-gradle-plugin (MAPS_API_KEY)
    implementation("com.google.android.gms:play-services-maps:20.0.0")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
