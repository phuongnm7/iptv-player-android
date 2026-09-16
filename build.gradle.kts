plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

// Legacy SmartTube modules use `apply plugin:` and need their plugin classpath
// available before each legacy project build script is evaluated.
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.13.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
    }
}

// SmartTube's legacy Gradle scripts read these through Gradle extra properties.
gradle.extensions.extraProperties.set("sharedModulesRoot", file("third_party/SmartTube-droid/SharedModules"))
gradle.extensions.extraProperties.set("sharedModulesConstants", file("third_party/SmartTube-droid/SharedModules/constants.gradle"))
gradle.extensions.extraProperties.set("mediaServiceCoreRoot", file("third_party/SmartTube-droid/MediaServiceCore"))
gradle.extensions.extraProperties.set("exoplayerRoot", file("third_party/SmartTube-droid/exoplayer-amzn-2.10.6"))
gradle.extensions.extraProperties.set("exoplayerModulePrefix", "exoplayer-")

// This hook runs before each project is evaluated, so legacy `apply plugin:`
// statements can resolve the Android/Kotlin plugins during script evaluation.
gradle.beforeProject {
    if (name != rootProject.name) {
        buildscript.repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        buildscript.dependencies {
            add("classpath", "com.android.tools.build:gradle:8.13.2")
            add("classpath", "org.jetbrains.kotlin:kotlin-gradle-plugin:1.8.10")
        }
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
