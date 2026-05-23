// 完整 Android library build 入口（含 :core 子專案）。
// 注意：此 full build 需 AGP + Android SDK，屬裝置/發佈階段；本機 CI 目前只跑 core/ 的純 JVM 單元測試
// （cd mobile/android/core && gradle test，免 AGP）。core/ 另有獨立 settings.gradle.kts 供 standalone 測試。
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.library") version "8.5.2"
        id("org.jetbrains.kotlin.android") version "2.0.21"
        kotlin("jvm") version "2.0.21"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "topadroi-android"
include(":core")
