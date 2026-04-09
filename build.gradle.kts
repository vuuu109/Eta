buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${project.property("kotlinGradlePluginVersion")}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
