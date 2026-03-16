plugins {
    id("com.android.application") version "9.1.0" apply false
    id("com.android.library") version "9.1.0" apply false
    // google-services declared here — applied in :app only after google-services.json
    // is present (obtained from Firebase Console — see KNOWN_ISSUES.md).
    id("com.google.gms.google-services") version "4.4.4" apply false
}
