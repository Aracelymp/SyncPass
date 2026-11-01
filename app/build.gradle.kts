import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val envFile = rootProject.file(".env")
val envProps = Properties()
if (envFile.exists()) {
    envFile.inputStream().use { envProps.load(it) }
}
// Prioridad: -PapiBaseUrl > .env > variable de entorno > emulador local.
val apiBaseUrl: String =
    (findProperty("apiBaseUrl") as String?)?.takeIf { it.isNotBlank() }
        ?: envProps.getProperty("API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: System.getenv("API_BASE_URL")?.takeIf { it.isNotBlank() }
        ?: "http://192.168.1.6:8080/"

// Prioridad: -PcognitoClientId > .env > variable de entorno > placeholder.
val cognitoClientId: String =
    (findProperty("cognitoClientId") as String?)?.takeIf { it.isNotBlank() }
        ?: envProps.getProperty("COGNITO_CLIENT_ID")?.takeIf { it.isNotBlank() }
        ?: System.getenv("COGNITO_CLIENT_ID")?.takeIf { it.isNotBlank() }
        ?: "<APP_CLIENT_ID>"

// Prioridad: -PcognitoUserPoolDomain > .env > variable de entorno > placeholder.
val cognitoUserPoolDomain: String =
    (findProperty("cognitoUserPoolDomain") as String?)?.takeIf { it.isNotBlank() }
        ?: envProps.getProperty("COGNITO_USER_POOL_DOMAIN")?.takeIf { it.isNotBlank() }
        ?: System.getenv("COGNITO_USER_POOL_DOMAIN")?.takeIf { it.isNotBlank() }
        ?: "https://<USER_POOL_DOMAIN>.auth.us-east-1.amazoncognito.com"

android {
    namespace = "com.rojas.syncpass"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "com.rojas.syncpass"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "COGNITO_CLIENT_ID", "\"$cognitoClientId\"")
        buildConfigField("String", "COGNITO_USER_POOL_DOMAIN", "\"$cognitoUserPoolDomain\"")
        manifestPlaceholders["appAuthRedirectScheme"] = "com.rojas.syncpass"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        compose = true
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.browser)
    implementation(libs.appauth)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.barcode.scanning)
    implementation(libs.zxing.core)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.coroutines.core)
    implementation(libs.coroutines.android)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
