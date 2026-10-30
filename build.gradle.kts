plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
}

// OneDrive may turn generated build artifacts into cloud placeholders, which
// breaks Gradle snapshots. Set SYNCPASS_BUILD_DIR to build outside the synced
// folder when needed (e.g. C:\Users\<user>\AppData\Local\Temp\syncpass-build).
System.getenv("SYNCPASS_BUILD_DIR")?.takeIf { it.isNotBlank() }?.let { external ->
    allprojects {
        layout.buildDirectory.set(file(external).resolve(project.name))
    }
}
