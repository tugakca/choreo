package io.github.tugakca.choreo.buildlogic

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

/** Formats Kotlin sources and build scripts with ktlint; rules come from the root .editorconfig. */
internal fun Project.configureSpotless() {
    apply(plugin = "com.diffplug.spotless")
    val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(ktlintVersion)
            licenseHeaderFile(rootProject.file("gradle/license-header.txt"))
        }
        kotlinGradle {
            target("*.kts")
            ktlint(ktlintVersion)
        }
    }
}
