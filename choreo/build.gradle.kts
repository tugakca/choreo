plugins {
    alias(libs.plugins.choreo.android.library)
    `maven-publish`
}

group = "com.github.tugakca.choreo"
version = "0.1.0"

kotlin {
    explicitApi()
}

android {
    namespace = "io.github.tugakca.choreo"

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.animation.core)
    api(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.graphics.shapes)
    implementation(libs.androidx.lifecycle.runtimeCompose)

    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    testImplementation(libs.androidx.compose.ui.testManifest)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                artifactId = "choreo"
                from(components["release"])
                pom {
                    name = "Choreo"
                    description = "Shape drawing and animation choreography for Jetpack Compose."
                    url = "https://github.com/tugakca/Choreo"
                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }
                    scm {
                        url = "https://github.com/tugakca/Choreo"
                    }
                }
            }
        }
    }
}
