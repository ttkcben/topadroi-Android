// topadroi Android SDK — Gradle 模組（發佈到 Maven Central: io.topadroi:sdk）
plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.topadroi.sdk"
    compileSdk = 34
    defaultConfig {
        minSdk = 21
        consumerProguardFiles("consumer-rules.pro")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(project(":core"))  // 平台無關核心邏輯（JVM 已測）
    // GAID（選配，production 啟用）：com.google.android.gms:play-services-ads-identifier
    // Install Referrer（選配）：com.android.installreferrer:installreferrer
    testImplementation("junit:junit:4.13.2")
}

// Maven 發佈設定。實際發佈到 Maven Central 需 Sonatype OSSRH 帳號 + GPG 簽章（見 mobile/PUBLISHING.md）。
afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "io.topadroi"
                artifactId = "sdk"
                version = "0.1.0"
                pom {
                    name.set("topadroi Android SDK")
                    description.set("Capture conversions and maximize ad ROI — lightweight event SDK that forwards to topadroi server-side.")
                    url.set("https://www.topadroi.com/developers")
                    licenses { license { name.set("Proprietary") } }
                    // SCM 指向 SDK 專屬公開 repo（非內部 monorepo）。
                    scm {
                        url.set("https://github.com/ttkcben/topadroi-Android")
                        connection.set("scm:git:https://github.com/ttkcben/topadroi-Android.git")
                    }
                }
            }
        }
    }
}
