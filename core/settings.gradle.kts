// 獨立 pure-Kotlin JVM 專案：只測純邏輯（無 Android 依賴），免 AGP。
// 完整 Android library build 另由 mobile/android/settings.gradle.kts 統籌（含 :core + library）。
// kotlin 版本放 pluginManagement → build.gradle.kts 不帶 version，使 core/ 可同時作 standalone 與 subproject（免 plugin 版本衝突）。
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        kotlin("jvm") version "2.0.21"
    }
}

rootProject.name = "topadroi-core"
