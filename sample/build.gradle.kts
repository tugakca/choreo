plugins {
    alias(libs.plugins.choreo.android.application)
}

android {
    namespace = "io.github.tugakca.choreo.sample"

    defaultConfig {
        applicationId = "io.github.tugakca.choreo.sample"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

dependencies {
    implementation(project(":choreo"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material.iconsCore)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptiveNavigationSuite)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
