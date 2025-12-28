plugins {
    id("com.android.application")
}

android {
    namespace = "itf.com.app.lms"
    compileSdk = 34

    defaultConfig {
        applicationId = "itf.com.app.simple_line_test_ovio_new"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    sourceSets {
        getByName("main") {
            res {
                srcDirs("src\\main\\res", "src\\main\\res\\assets")
            }
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true  // BuildConfig 클래스 생성 활성화
    }
}

dependencies {

    implementation("com.google.android.material:material:1.4.0")

    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
//    implementation("com.github.mik3y:usb-serial-for-android:3.7.0")
    implementation("com.github.felHR85:UsbSerial:6.0.6")
//    implementation("com.github.felHR85:UsbSerial:6.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("net.sourceforge.jexcelapi:jxl:2.6.12")
    implementation("androidx.work:work-runtime:2.7.1")
    implementation("androidx.navigation:navigation-fragment:2.7.5")
    implementation("androidx.navigation:navigation-ui:2.7.5")
    implementation("com.nanohttpd:nanohttpd-webserver:2.1.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation("androidx.work:work-runtime:2.7.1")

    // define a BOM and its version
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.9.1"))

    // define any required OkHttp artifacts without version
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")
    
    // MPAndroidChart 라이브러리 추가
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}