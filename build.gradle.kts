// Every plugin used by a subproject is declared here so that all subprojects share one build
// script classpath. This avoids version mismatches between transitive plugin dependencies.
plugins {
    alias(libs.plugins.binary.compatibility.validator)
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.spotless) apply false
}

apiValidation {
    // The sample is an app, not a library, so it has no public API to protect.
    ignoredProjects += "sample"
}
