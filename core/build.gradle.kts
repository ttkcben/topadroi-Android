// 純 Kotlin/JVM 模組：topadroi SDK 的平台無關核心邏輯（事件組裝 / user_data / offline queue）。
// JVM 單元測試（JUnit），免 Android SDK / AGP。Android library 以 implementation(project(":core")) 引用。
plugins {
    kotlin("jvm")   // 版本由 settings.gradle.kts pluginManagement 提供（dual-use：standalone + subproject）
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}

tasks.test {
    useJUnit()
    testLogging { events("passed", "failed", "skipped") }
}
