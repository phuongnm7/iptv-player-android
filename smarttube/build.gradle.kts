plugins {
    id("com.android.library") version "8.13.2"
}

android {
    namespace = "com.liskovsoft.smartyoutubetv2.droid"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36
        multiDexEnabled = true
        missingDimensionStrategy("default", "ststable")
        missingDimensionStrategy("device", "mobile")
        buildConfigField("long", "TIMESTAMP", "${System.currentTimeMillis()}L")
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    sourceSets["main"].apply {
        java.srcDirs("../third_party/SmartTube-droid/smarttubedroid/src/main/java")
        res.srcDirs("../third_party/SmartTube-droid/smarttubedroid/src/main/res")
        assets.srcDirs("../third_party/SmartTube-droid/smarttubedroid/src/main/assets")
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "../third_party/SmartTube-droid/smarttubedroid/libs", "include" to listOf("*.jar"))))

    implementation(project(":common"))
    implementation(project(":sharedutils"))
    implementation(project(":mediaserviceinterfaces"))
    implementation(project(":youtubeapi"))
    implementation(project(":fragment-1.1.0"))
    implementation(project(":exoplayer-library-core"))
    implementation(project(":exoplayer-library-ui"))
    implementation(project(":exoplayer-extension-mediasession"))
    implementation(project(":doubletapplayerview"))

    // Native SmartTube Droid phone runtime dependencies, matching upstream.
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.2")
    implementation("androidx.media:media:1.2.0")
    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.android.material:material:1.5.0")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    implementation("com.github.bumptech.glide:annotations:4.11.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.11.0")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("org.conscrypt:conscrypt-android:2.5.3")
}
