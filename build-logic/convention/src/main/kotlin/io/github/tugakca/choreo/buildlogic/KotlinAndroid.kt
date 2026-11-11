package io.github.tugakca.choreo.buildlogic

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val COMPILE_SDK = 37
internal const val TARGET_SDK = 36
internal const val MIN_SDK = 23

internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = COMPILE_SDK
        defaultConfig.minSdk = MIN_SDK
        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
    }
    configure<KotlinAndroidProjectExtension> {
        // Opt in to treating warnings as errors with -PwarningsAsErrors=true (CI does this).
        val warningsAsErrors = providers.gradleProperty("warningsAsErrors")
            .map(String::toBoolean)
            .orElse(false)
        compilerOptions.apply {
            jvmTarget = JvmTarget.JVM_17
            allWarningsAsErrors = warningsAsErrors
        }
    }
}
