plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

// Make the pinned SmartTube/SharedModules version constants available to their
// legacy Groovy Android library build scripts without using SmartTube's own root build.
gradle.ext.sharedModulesRoot = file("third_party/SmartTube-droid/SharedModules")
gradle.ext.sharedModulesConstants = file("third_party/SmartTube-droid/SharedModules/constants.gradle")
gradle.ext.mediaServiceCoreRoot = file("third_party/SmartTube-droid/MediaServiceCore")
gradle.ext.exoplayerRoot = file("third_party/SmartTube-droid/exoplayer-amzn-2.10.6")
gradle.ext.exoplayerModulePrefix = "exoplayer-"

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
