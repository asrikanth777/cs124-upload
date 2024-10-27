@file:Suppress("SpellCheckingInspection", "MagicNumber", "GradleDependency", "UnstableApiUsage")

import java.util.UUID
import java.util.function.BiConsumer

/*
 * This file configures the build system that creates your Android app.
 * The syntax is Kotlin, but many of the idioms are probably unfamiliar.
 * You do not need to understand the contents of this file, nor should you modify it.
 * Any changes will be overwritten during official grading.
 */

buildscript {
    repositories {
        mavenCentral()
        google()
    }
}
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.github.cs124-illinois.gradlegrader") version "2023.3.1"
    id("io.gitlab.arturbosch.detekt") version "1.22.0"
    id("org.jlleitschuh.gradle.ktlint")
}
android {
    compileSdk = 33
    defaultConfig {
        applicationId = "edu.illinois.cs.cs124.ay2022.favoriteplaces"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
    namespace = "edu.illinois.cs.cs124.ay2022.mp"
    sourceSets {
        map { it.java.srcDir("src/${it.name}/kotlin") }
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}
dependencies {
    /*
     * Do not add dependencies here, since they will be overwritten during official grading.
     * If you have a package that you think would be broadly useful for completing the MP, please start a discussion
     * on the forum.
     */

    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.10")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.android.volley:volley:1.2.0")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.14.2")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.squareup.okhttp3:mockwebserver:4.10.0")
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    implementation("com.opencsv:opencsv:5.7.1")

    testImplementation("com.github.cs124-illinois:gradlegrader:2023.3.1")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.9.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test.ext:truth:1.5.0")
    testImplementation("androidx.test.espresso:espresso-core:3.5.1")
    testImplementation("androidx.test.espresso:espresso-intents:3.5.1")
    testImplementation("androidx.test.espresso:espresso-contrib:3.5.1")
}
detekt {
    toolVersion = "1.22.0"
    config = files("config/detekt/detekt.yml")
    buildUponDefaultConfig = true
}
gradlegrader {
    assignment = "AY2022.MP"
    checkpoint {
        yamlFile = rootProject.file("grade.yaml")
        configureTests(
            BiConsumer { MP, test ->
                require(MP in setOf("0", "1", "2", "3")) { "Cannot grade unknown checkpoint MP$MP" }
                test.setTestNameIncludePatterns(listOf("MP${MP}Test"))
                test.filter.isFailOnNoMatchingTests = true
            }
        )
    }
    detekt {
        points = 10
    }
    forceClean = false
    identification {
        txtFile = rootProject.file("ID.txt")
        @Suppress("SwallowedException")
        validate = Spec {
            try {
                UUID.fromString(it.trim())
                true
            } catch (e: java.lang.IllegalArgumentException) {
                false
            }
        }
    }
    reporting {
        post {
            endpoint = "https://cloud.cs124.org/gradlegrader"
        }
        printPretty {
            title = "Grade Summary"
            notes = "On checkpoints with an early deadline, the maximum local score is 90/100. " +
                "10 points will be provided during official grading if you submit code " +
                "that meets the early deadline threshold before the early deadline."
        }
    }
    vcs {
        git = true
        requireCommit = true
    }
}
