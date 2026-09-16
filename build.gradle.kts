plugins {
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    id("org.jetbrains.kotlin.android") version "1.8.10" apply false
}

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

// The legacy SmartTube modules expect constants.gradle to run before their
// own build.gradle files so project.properties.compileSdkVersion, minSdkVersion,
// targetSdkVersion and the shared dependency versions already exist.
val smartTubeSharedConstantsProjects = setOf(
    ":common",
    ":leanbackassistant",
    ":leanback-1.0.0",
    ":fragment-1.1.0",
    ":filepicker-lib",
    ":doubletapplayerview",
    ":slidableactivity",
    ":sharedutils",
    ":sharedtests",
    ":appupdatechecker2",
    ":commons-io-2.8.0",
    ":j2v8"
)

gradle.beforeProject(org.gradle.api.Action<org.gradle.api.Project> { project ->
    if (project.path in smartTubeSharedConstantsProjects) {
        project.apply(
            mapOf(
                "from" to project.rootProject.file(
                    "third_party/SmartTube-droid/SharedModules/constants.gradle"
                )
            )
        )
    }
})

// These vendored SmartTube modules are Android libraries but use legacy
// `apply plugin:` scripts. Apply the Android/Kotlin plugins from the root
// so they expose variants before :smarttube resolves their dependencies.
val smartTubeAndroidLibraryModules = setOf(
    ":common",
    ":leanbackassistant",
    ":leanback-1.0.0",
    ":fragment-1.1.0",
    ":filepicker-lib",
    ":doubletapplayerview",
    ":slidableactivity",
    ":sharedutils",
    ":sharedtests",
    ":appupdatechecker2",
    ":mediaserviceinterfaces",
    ":youtubeapi",
    ":exoplayer-library",
    ":exoplayer-library-core",
    ":exoplayer-library-dash",
    ":exoplayer-library-sabr",
    ":exoplayer-library-hls",
    ":exoplayer-library-smoothstreaming",
    ":exoplayer-library-ui",
    ":exoplayer-extension-mediasession",
    ":exoplayer-extension-okhttp",
    ":exoplayer-extension-cronet"
)

subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
