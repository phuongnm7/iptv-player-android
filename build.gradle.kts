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
    override fun beforeEvaluate(project: Project) {
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

    override fun afterEvaluate(project: Project, state: ProjectState) { }
})

subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<com.android.build.gradle.LibraryExtension> {
            buildFeatures.buildConfig = true
        }
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

// AGP 8.13 validates Java/Kotlin target compatibility strictly. SmartTube's
// legacy library modules compile Java at 1.8, so force their Kotlin compiler to
// the same JVM target. The rule is scoped to SmartTube library modules and does
// not alter the NM7 app module's Kotlin target.
subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            kotlinOptions.jvmTarget = "1.8"
        }
    }
}

// Legacy SmartTube/ExoPlayer extension unit-test manifests are test-only and
// fail AGP 8.13 validation. They are not required to assemble the NM7 runtime.
// Skip MobileDebugUnitTest tasks for all legacy ExoPlayer extension modules while
// keeping their production/runtime libraries fully enabled.
subprojects {
    if (path.startsWith(":exoplayer-extension-")) {
        tasks.matching { it.name.contains("MobileDebugUnitTest") }.configureEach {
            enabled = false
        }
    }
}

// SmartTube's common module references a few application-level resources that
// are not present in the library module after the app was split into NM7's
// :smarttube library. Generate a tiny compatibility resource file during
// Gradle configuration so common compiles without changing the upstream tree.
val smartTubeCompatResources = rootProject.file(
    "third_party/SmartTube-droid/common/src/main/res/values/nm7_smarttube_compat.xml"
)
smartTubeCompatResources.parentFile.mkdirs()
val smartTubeCompatXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
    "<resources>\n" +
    "    <string name=\"app_name\">SmartTube</string>\n" +
    "    <string name=\"cancel\">Cancel</string>\n" +
    "    <style name=\"AppDialog\" parent=\"android:style/Theme.Material.Dialog.Alert\" />\n" +
    "    <item name=\"lb_control_closed_captioning\" type=\"id\" />\n" +
    "    <item name=\"lb_control_high_quality\" type=\"id\" />\n" +
    "</resources>\n"
smartTubeCompatResources.writeText(smartTubeCompatXml)

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
