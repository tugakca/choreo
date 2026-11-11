import com.android.build.api.dsl.ApplicationExtension
import io.github.tugakca.choreo.buildlogic.TARGET_SDK
import io.github.tugakca.choreo.buildlogic.configureKotlinAndroid
import io.github.tugakca.choreo.buildlogic.configureSpotless
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

/** Android application with Compose enabled and the shared Kotlin/formatting setup. */
abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "org.jetbrains.kotlin.android")
            apply(plugin = "org.jetbrains.kotlin.plugin.compose")
            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                buildFeatures.compose = true
                defaultConfig.targetSdk = TARGET_SDK
            }
            configureSpotless()
        }
    }
}
