import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)

}

android {
    namespace = "com.example.cookpal"
    compileSdk = 34

    buildFeatures {
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.example.cookpal"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        // Load the OpenAI API key from local.properties
        val localPropertiesFile = rootProject.file("local.properties")
        val openApiKey = if (localPropertiesFile.exists()) {
            Properties().apply {
                load(localPropertiesFile.inputStream()) // Now recognized
            }.getProperty("OPENAI_API_KEY") ?: ""
        } else {
            ""
        }

        buildConfigField("String", "OPENAI_API_KEY", "\"$openApiKey\"")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            isMinifyEnabled = false
        }
    }
    packagingOptions {
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation (libs.picasso)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.auth.ktx)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.espresso.core)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation (libs.material.v190)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit.v2110)
    implementation (libs.androidx.core.ktx.v170)
    implementation (libs.androidx.appcompat.v141)
    implementation(libs.converter.gson.v2110)
    implementation (libs.firebase.auth.v2110)
    implementation (libs.firebase.database.v2030)
    implementation (libs.firebase.auth.v2200)
    implementation (libs.play.services.auth.v2120)
    implementation (libs.okhttp)
    implementation (libs.kotlinx.coroutines.core.v160)
    implementation (libs.kotlinx.coroutines.android.v160)
    implementation ("com.sendgrid:sendgrid-java:4.9.3") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }
    implementation(libs.androidx.espresso.core)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.firestore.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    implementation (libs.material)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.retrofit.v2110)
    implementation(libs.converter.gson.v2110)
    implementation (libs.firebase.auth.v2310)
    implementation (libs.firebase.database)
    implementation (libs.firebase.auth.v2310)
    implementation (libs.play.services.auth.v2120)
    implementation (libs.okhttp)
    implementation (libs.kotlinx.coroutines.core.v160)
    implementation (libs.kotlinx.coroutines.android)
    implementation ("com.sendgrid:sendgrid-java:4.9.3") {
        exclude(group = "org.apache.httpcomponents", module = "httpclient")
    }

}