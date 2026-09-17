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
    ":common", ":leanbackassistant", ":leanback-1.0.0", ":fragment-1.1.0", ":filepicker-lib",
    ":doubletapplayerview", ":slidableactivity", ":sharedutils", ":sharedtests", ":mediaserviceinterfaces",
    ":youtubeapi", ":exoplayer-library", ":exoplayer-library-core", ":exoplayer-library-dash",
    ":exoplayer-library-sabr", ":exoplayer-library-hls", ":exoplayer-library-smoothstreaming",
    ":exoplayer-library-ui", ":exoplayer-extension-mediasession", ":exoplayer-extension-okhttp",
    ":exoplayer-extension-cronet"
)

val smartTubeSharedConstantsProjects = setOf(
    ":common", ":leanbackassistant", ":leanback-1.0.0", ":fragment-1.1.0", ":filepicker-lib",
    ":doubletapplayerview", ":slidableactivity", ":sharedutils", ":sharedtests", ":appupdatechecker2",
    ":commons-io-2.8.0", ":j2v8"
)

gradle.addProjectEvaluationListener(object : org.gradle.api.ProjectEvaluationListener {
    override fun beforeEvaluate(project: Project) {
        if (project.path in smartTubeSharedConstantsProjects) {
            project.apply(mapOf("from" to project.rootProject.file("third_party/SmartTube-droid/SharedModules/constants.gradle")))
        }
    }
    override fun afterEvaluate(project: Project, state: ProjectState) { }
})

subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        pluginManager.apply("com.android.library")
        pluginManager.apply("org.jetbrains.kotlin.android")
        extensions.configure<com.android.build.gradle.LibraryExtension> { buildFeatures.buildConfig = true }
    }
}

subprojects {
    tasks.matching { it.name.endsWith("Kotlin") }.configureEach {
        doFirst {
            val source = rootProject.file("third_party/SmartTube-droid/SharedModules/sharedutils/src/main/java/com/liskovsoft/sharedutils/helpers/DohProviders.kt")
            if (source.isFile) {
                var text = source.readText()
                if (text.contains("HttpUrl.parse(s)")) {
                    text = text.replace("HttpUrl.parse(s)", "HttpUrl.get(s)")
                    source.writeText(text)
                    logger.lifecycle("Patched SmartTube DohProviders.kt: HttpUrl.parse -> HttpUrl.get")
                }
            }
        }
    }
}

subprojects {
    if (path in smartTubeAndroidLibraryModules) {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach { kotlinOptions.jvmTarget = "1.8" }
    }
}

subprojects {
    if (path.startsWith(":exoplayer-")) {
        tasks.matching { it.name.contains("MobileDebugUnitTest") }.configureEach { enabled = false }
    }
}

// SmartTube's common library references these legacy app resources directly.
// They must live in common, because common compiles those Java sources.
val smartTubeCompatResources = rootProject.file("third_party/SmartTube-droid/common/src/main/res/values/nm7_smarttube_compat.xml")
smartTubeCompatResources.parentFile.mkdirs()
val smartTubeCompatXml = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="app_name">SmartTube</string>
    <string name="cancel">Cancel</string>
    <style name="AppDialog" parent="android:style/Theme.Material.Dialog.Alert" />
    <item name="lb_control_closed_captioning" type="id" />
    <item name="lb_control_high_quality" type="id" />
</resources>
""".trimIndent() + "\n"
smartTubeCompatResources.writeText(smartTubeCompatXml)

// SmartTube phone Activities compile against their own R class. These two
// strings are therefore provided in the SmartTube library as well.
val smartTubePhoneResources = rootProject.file("third_party/SmartTube-droid/smarttubedroid/src/main/res/values/nm7_smarttube_phone_compat.xml")
smartTubePhoneResources.parentFile.mkdirs()
val smartTubePhoneXml = """
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="signin_view_description">Sign in to SmartTube %1$s</string>
    <string name="action_search">Search</string>
</resources>
""".trimIndent() + "\n"
smartTubePhoneResources.writeText(smartTubePhoneXml)

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
