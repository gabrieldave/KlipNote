import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.blackeye.klipnote"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.blackeye.klipnote"
        minSdk = 26
        targetSdk = 35

        // Fuente de verdad única (local). CI no sobrescribe con GITHUB_RUN_NUMBER.
        val localVersionCode = 141
        val localVersionName = "1.9.3"
        versionCode = localVersionCode
        versionName = localVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Cero llaves en el APK: cada usuario pone las suyas (BYOK cifrado en el dispositivo).

        // Config remota opcional: config/stt-router.json del repo con el que se compila.
        // En GitHub Actions se arma sola con GITHUB_REPOSITORY (sirve también para forks).
        // En local: define klipnote.githubRepo=usuario/repo en gradle.properties (o déjalo vacío).
        val configRepo = System.getenv("GITHUB_REPOSITORY")
            ?: (project.findProperty("klipnote.githubRepo") as String?).orEmpty()
        val configUrl = if (configRepo.isBlank()) "" else
            "https://raw.githubusercontent.com/$configRepo/main/config/stt-router.json"
        buildConfigField("String", "REMOTE_CONFIG_URL", "\"$configUrl\"")

        // libpython / libffmpeg / aria2c (Seal / youtubedl-android)
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    signingConfigs {
        create("release") {
            val ciPassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
            if (!ciPassword.isNullOrBlank()) {
                val ciPath = System.getenv("ANDROID_KEYSTORE_PATH")
                require(!ciPath.isNullOrBlank()) {
                    "ANDROID_KEYSTORE_PATH requerido cuando ANDROID_KEYSTORE_PASSWORD está definido"
                }
                storeFile = file(ciPath)
                storePassword = ciPassword
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = ciPassword
            } else {
                val propsPath = System.getenv("ANDROID_KEYSTORE_PROPS")
                val propsFile = if (!propsPath.isNullOrBlank()) file(propsPath) else null
                if (propsFile != null && propsFile.exists()) {
                    val props = Properties()
                    propsFile.inputStream().use { props.load(it) }
                    storeFile = file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigning = signingConfigs.getByName("release")
            if (releaseSigning.storeFile != null) {
                signingConfig = releaseSigning
            }
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.annotation:annotation:1.9.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Seal / junkfood02 fork — 0.18.1 incluye QuickJS (libqjs.so) para YouTube EJS
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:aria2c:0.18.1")

    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
