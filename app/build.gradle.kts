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
        versionCode = 130
        versionName = "1.10.114"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["applicationClass"] = "vn.phuong.iptvplayer.MobileNm7Application"
        missingDimensionStrategy("default", "ststable")
    }

    flavorDimensions += "device"
    productFlavors {
        create("mobile") {
            dimension = "device"
            manifestPlaceholders["vlcFallbackEnabled"] = "false"
            manifestPlaceholders["applicationClass"] = "vn.phuong.iptvplayer.MobileNm7Application"
        }
    }

    if (project.findProperty("mobileAbiSplits") == "true") {
        splits {
            abi {
                isEnable = true
                reset()
                include("armeabi-v7a", "arm64-v8a")
                isUniversalApk = false
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions { unitTests.isIncludeAndroidResources = true }

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
    implementation("androidx.multidex:multidex:2.0.1")
    add("mobileImplementation", project(":smarttube"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.14.1")
    testImplementation("org.json:json:20240303")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}
