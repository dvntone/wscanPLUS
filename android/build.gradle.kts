plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    // google-services — applied in :app; requires google-services.json at android/app/
    id("com.google.gms.google-services") version "4.4.4" apply false
    // secrets-gradle-plugin: injects GOOGLE_MAPS_API_KEY and CROWDSEC_CTI_API_KEY
    // from local.properties into BuildConfig/manifest.
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
    // ktlint — Kotlin linter, zero-config. Applied per-module below.
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("androidx.room") version "2.8.4" apply false
}
