// NM7 Mobile 2.0 feature branch build.
plugins {
    id("com.android.application")
}

val nm7IconSource = layout.projectDirectory.file("tools/nm7_app_icon.jpg.b64")
val nm7IconResDir = layout.buildDirectory.dir("generated/res/nm7AppIcon")

val generateNm7AppIcon by tasks.registering {
    inputs.file(nm7IconSource)
    val output = nm7IconResDir.map { it.file("drawable-nodpi/nm7_app_icon_new.jpg") }
    outputs.file(output)
    doLast {
        val out = output.get().asFile
        out.parentFile.mkdirs()
        val encoded = nm7IconSource.asFile.readText().filterNot { it.isWhitespace() }
        out.writeBytes(java.util.Base64.getDecoder().decode(encoded))
    }
}

android {
    namespace = "vn.phuong.iptvplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.phuong.iptvplayer.mobile2"
        minSdk = 23
        targetSdk = 36
        versionCode = 46
        versionName = "2.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions += "device"
    productFlavors {
        create("mobile") {
            dimension = "device"
            versionNameSuffix = "-mobile"
            manifestPlaceholders["vlcFallbackEnabled"] = "false"
        }
        create("tv") {
            dimension = "device"
            versionNameSuffix = "-tv"
            manifestPlaceholders["vlcFallbackEnabled"] = "true"
        }
    }

    sourceSets["main"].res.srcDir(nm7IconResDir)

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        disable += setOf("HardcodedText", "SetTextI18n", "MissingTranslation", "LockedOrientationActivity")
    }
}

tasks.configureEach {
    if (name.startsWith("pre") && name.endsWith("Build")) {
        dependsOn(generateNm7AppIcon)
    }
}

dependencies {
    val media3Version = "1.11.0"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
    implementation("androidx.media3:media3-exoplayer-smoothstreaming:$media3Version")
    implementation("androidx.media3:media3-exoplayer-rtsp:$media3Version")
    implementation("androidx.media3:media3-datasource-rtmp:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-datasource-okhttp:$media3Version")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    compileOnly("org.videolan.android:libvlc-all:3.6.1")
    add("tvImplementation", "org.videolan.android:libvlc-all:3.6.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
