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

gradle.addProjectEvaluationListener(object : org.gradle.api.ProjectEvaluationListener {
    override fun beforeEvaluate(project: org.gradle.api.Project) {
        if (project.path in smartTubeSharedConstantsProjects) {
            project.apply(
                mapOf(
                    "from" to project.rootProject.file(
                        "third_party/SmartTube-droid/SharedModules/constants.gradle"
                    )
                )
            )
        }
    }

    override fun afterEvaluate(
        project: org.gradle.api.Project,
        state: org.gradle.api.ProjectState
    ) { }
})

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
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<com.android.build.gradle.LibraryExtension> {
                flavorDimensions += "device"
                if (productFlavors.findByName("mobile") == null) {
                    productFlavors.create("mobile") {
                        dimension = "device"
                    }
                }
            }
        }
    }
    if (path == ":appupdatechecker2") {
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<com.android.build.gradle.LibraryExtension> {
                namespace = "com.liskovsoft.appupdatechecker2"
            }
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
