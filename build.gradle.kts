// Top-level build file where you can add configuration options common to all sub-projects/modules.



plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.google.gms.google.services) apply false
}
buildscript {
    dependencies {
        // Add the classpath for Firebase services and Google services plugin
        classpath("com.google.gms:google-services:4.3.15")  // Google Services plugin for Firebase integration
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.5")  // Firebase Crashlytics plugin
    }
}