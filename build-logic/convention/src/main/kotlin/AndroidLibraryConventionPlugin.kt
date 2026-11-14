import com.android.build.api.dsl.LibraryExtension
import io.github.tugakca.choreo.buildlogic.TARGET_SDK
import io.github.tugakca.choreo.buildlogic.configureKotlinAndroid
import io.github.tugakca.choreo.buildlogic.configureSpotless
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** Android library with Compose enabled and the shared Kotlin/formatting setup. */
abstract class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            extensions.configure<LibraryExtension> {
                configureKotlinAndroid(this)
                buildFeatures.compose = true
                testOptions.targetSdk = TARGET_SDK
                lint.targetSdk = TARGET_SDK
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            configureSpotless()
        }
    }
}
