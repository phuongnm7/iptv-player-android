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
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")
}

// Wire the real SleepTimer implementation into the compiled Mobile APK.
tasks.register("injectSleepTimerFeature") {
    doLast {
        val file = file("src/main/java/vn/phuong/iptvplayer/MainActivity.java")
        var text = file.readText()

        val oldOnCreate = "@Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); restoreSession(); epgHandler.post(epgTick); }"
        val newOnCreate = "@Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); SleepTimer.restore(this); restoreSession(); epgHandler.post(epgTick); }"
        if (!text.contains("SleepTimer.restore(this);")) text = text.replace(oldOnCreate, newOnCreate)

        val oldItems = "\"Quản lý nguồn IPTV\",\"Thêm hoặc mở URL/tệp\",\"Tải lại playlist hiện tại\",\"Lịch phát sóng (EPG)\",\"Giao diện: \"+modeLabel"
        val newItems = "\"Quản lý nguồn IPTV\",\"Thêm hoặc mở URL/tệp\",\"Tải lại playlist hiện tại\",\"Lịch phát sóng (EPG)\",\"Hẹn giờ đóng app\",\"Giao diện: \"+modeLabel"
        if (!text.contains("\"Hẹn giờ đóng app\"")) {
            text = text.replace(oldItems, newItems)
            text = text.replace("if(which==3)showEpgEditor();\n                    if(which==4)chooseInterfaceMode();", "if(which==3)showEpgEditor();\n                    if(which==4){SleepTimer.showDialog(this);return;}\n                    if(which==5)chooseInterfaceMode();")
            text = text.replace("if(which==5)chooseWallpaper();", "if(which==6)chooseWallpaper();")
            text = text.replace("if(which==6){AppPreferences.setShowUrls", "if(which==7){AppPreferences.setShowUrls")
            text = text.replace("if(which==7){AppPreferences.setCompactRows", "if(which==8){AppPreferences.setCompactRows")
            text = text.replace("if(which==8)AppPreferences.setShowFps", "if(which==9)AppPreferences.setShowFps")
            text = text.replace("if(which==9)AppPreferences.setShowClock", "if(which==10)AppPreferences.setShowClock")
            text = text.replace("if(which==10)AppPreferences.setShowPlayerSource", "if(which==11)AppPreferences.setShowPlayerSource")
        }

        file.writeText(text)
    }
}

tasks.named("preMobileDebugBuild") {
    dependsOn("injectSleepTimerFeature")
}
