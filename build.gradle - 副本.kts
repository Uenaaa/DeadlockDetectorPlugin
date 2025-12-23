plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.23"
    id("org.jetbrains.intellij") version "1.17.4"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.deadlock.detector"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://cache-redirector.jetbrains.com/intellij-repository/releases")
    }
    // DevEco Studio专属仓库
    maven("https://repo.huaweicloud.com/repository/maven/")
    maven("https://developer.huawei.com/repo/")
    // 新增：Compose Multiplatform专属仓库
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// JDK版本配置
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// DevEco Studio适配配置
intellij {
    // 对应DevEco Studio 6.0.1的底层IntelliJ版本
    version.set("2024.2.4")
    type.set("IC") // 保持社区版类型
    downloadSources.set(false)
    // 移除插件依赖，避免ID冲突
}

// 插件构建任务配置
tasks {
    buildPlugin {
        archiveBaseName.set("DeadLockDetectorPlugin")
        archiveVersion.set(version.toString())
    }

    // 可选：添加编译任务的编码配置，避免中文乱码
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21" // 与Java版本保持一致
        }
    }
}