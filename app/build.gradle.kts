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
        versionCode = 43
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

tasks.register("injectSleepTimerFeature") {
    doLast {
        val source = file("src/main/java/vn/phuong/iptvplayer/MainActivity.java")
        var text = source.readText()
        if (!text.contains("SleepTimer.showDialog(this)")) {
            val oldItems = "List<String> items=new ArrayList<>(java.util.Arrays.asList(\"Quản lý nguồn IPTV\",\"Thêm hoặc mở URL/tệp\",\"Tải lại playlist hiện tại\",\"Lịch phát sóng (EPG)\",\"Giao diện: \"+modeLabel,\"Đổi hình nền\",urls,rows,fps,clock,playerSource));"
            val newItems = "List<String> items=new ArrayList<>(java.util.Arrays.asList(\"Quản lý nguồn IPTV\",\"Thêm hoặc mở URL/tệp\",\"Tải lại playlist hiện tại\",\"Lịch phát sóng (EPG)\",\"Giao diện: \"+modeLabel,\"Đổi hình nền\",urls,rows,fps,clock,playerSource,\"Hẹn giờ đóng app\"));"
            check(text.contains(oldItems)) { "1.10.25 MainActivity settings block changed; sleep-timer injection aborted" }
            text = text.replace(oldItems, newItems)
            val oldHandler = "if(which==10)AppPreferences.setShowPlayerSource(this,!AppPreferences.showPlayerSource(this));"
            val newHandler = oldHandler + "\n                    if(which==11)SleepTimer.showDialog(this);"
            check(text.contains(oldHandler)) { "1.10.25 MainActivity player-source handler changed; sleep-timer injection aborted" }
            text = text.replace(oldHandler, newHandler)
            val oldCreate = "@Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); restoreSession(); epgHandler.post(epgTick); }"
            val newCreate = "@Override protected void onCreate(Bundle savedInstanceState) { super.onCreate(savedInstanceState); setupViews(); SleepTimer.restore(this); restoreSession(); epgHandler.post(epgTick); }"
            check(text.contains(oldCreate)) { "1.10.25 MainActivity onCreate changed; sleep-timer restore injection aborted" }
            text = text.replace(oldCreate, newCreate)
            source.writeText(text)
        }
    }
}

tasks.configureEach {
    if (name == "preMobileDebugBuild") dependsOn("injectSleepTimerFeature")
}
