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

gradle.extensions.extraProperties.set("sharedModulesRoot", file("third_party/SmartTube-droid/SharedModules"))
gradle.extensions.extraProperties.set("sharedModulesConstants", file("third_party/SmartTube-droid/SharedModules/constants.gradle"))
gradle.extensions.extraProperties.set("mediaServiceCoreRoot", file("third_party/SmartTube-droid/MediaServiceCore"))
gradle.extensions.extraProperties.set("exoplayerRoot", file("third_party/SmartTube-droid/exoplayer-amzn-2.10.6"))
gradle.extensions.extraProperties.set("exoplayerModulePrefix", "exoplayer-")

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

subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
    }
}

// SmartTube's pinned SharedModules still contains OkHttp 3-era API calls.
// Patch the source in CI immediately before Kotlin compilation so the nested
// submodule stays untouched while remaining compatible with the current OkHttp.
subprojects {
    tasks.matching { it.name.endsWith("Kotlin") }.configureEach {
        doFirst {
            val source = rootProject.file(
                "third_party/SmartTube-droid/SharedModules/sharedutils/src/main/java/com/liskovsoft/sharedutils/helpers/DohProviders.kt"
            )
            if (source.isFile) {
                var text = source.readText()
                if (text.contains("HttpUrl.parse(s)")) {
                    if (!text.contains("import okhttp3.toHttpUrlOrNull")) {
                        text = text.replaceFirst(
                            "import okhttp3.HttpUrl",
                            "import okhttp3.HttpUrl\nimport okhttp3.toHttpUrlOrNull"
                        )
                    }
                    text = text.replace("HttpUrl.parse(s)", "s.toHttpUrlOrNull()")
                    source.writeText(text)
                    logger.lifecycle("Patched SmartTube DohProviders.kt: HttpUrl.parse -> toHttpUrlOrNull")
                }
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
    configurations.all {
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
        resolutionStrategy.force("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
        resolutionStrategy.force("com.squareup.okhttp3:okhttp:3.12.13")
    }
}
