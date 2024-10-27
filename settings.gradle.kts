/*
 * This file configures the build system that creates your Android app.
 * The syntax is Kotlin, but many of the idioms are likely unfamiliar.
 * You do not need to understand the contents of this file, nor should you modify it.
 * Any changes will be overwritten during official grading.
 */

rootProject.name = "AY2022-MP-Kotlin"
include(":app")

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
