plugins {
    id("com.android.application")
}

android {
    namespace = "vn.phuong.iptvplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "vn.phuong.iptvplayer"
        minSdk = 23
        targetSdk = 36
        versionCode = 44
        versionName = "1.10.26"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // SmartTube's legacy modules retain the "default" flavor dimension
        // (stbeta/ststable/stfdroid). Select the stable runtime for NM7 Mobile.
        missingDimensionStrategy("default", "ststable")
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

    // Mobile is intentionally split into exactly the two ARM ABIs used by the
    // supported devices. Do not generate x86/x86_64 or a universal APK.
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a", "arm64-v8a")
            isUniversalApk = false
        }
    }

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

    // Native SmartTube phone runtime is packaged only into the Mobile flavor;
    // the TV flavor remains the existing NM7 TV implementation.
    add("mobileImplementation", project(":smarttube"))

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}