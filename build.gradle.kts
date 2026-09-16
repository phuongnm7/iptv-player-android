plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

// Legacy SmartTube modules use `apply plugin:` and therefore need the
// Android/Kotlin plugins on the buildscript classpath as well.
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

// SmartTube's legacy Groovy scripts read these through Gradle's extra properties.
gradle.extensions.extraProperties.set("sharedModulesRoot", file("third_party/SmartTube-droid/SharedModules"))
gradle.extensions.extraProperties.set("sharedModulesConstants", file("third_party/SmartTube-droid/SharedModules/constants.gradle"))
gradle.extensions.extraProperties.set("mediaServiceCoreRoot", file("third_party/SmartTube-droid/MediaServiceCore"))
gradle.extensions.extraProperties.set("exoplayerRoot", file("third_party/SmartTube-droid/exoplayer-amzn-2.10.6"))
gradle.extensions.extraProperties.set("exoplayerModulePrefix", "exoplayer-")

// The vendored SmartTube tree predates the plugins DSL and applies Android/Kotlin
// plugins from individual subproject build.gradle files. Gradle buildscript
// classpaths are project-scoped, so expose the legacy plugin classpath to every
// subproject explicitly.
subprojects {
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
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
