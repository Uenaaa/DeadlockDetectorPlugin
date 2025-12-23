plugins {
    id("org.jetbrains.intellij") version "1.17.2" // 固定插件版本
    kotlin("jvm") version "1.9.20"
    java
}

// 配置Kotlin编译选项
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

group = "com.deadlock.detector"
version = "1.0-SNAPSHOT"

// 鸿蒙/华为专属仓库，保证依赖下载
repositories {
    mavenCentral()
    maven("https://repo.huaweicloud.com/repository/maven/")
    maven("https://ohosdevrepo.obs.cn-east-2.myhuaweicloud.com/repository/maven/")
    maven("https://cache-redirector.jetbrains.com/plugins.jetbrains.com/maven")
}

// 使用与IDE兼容的JDK版本（Java 17）
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2023.1") // 使用IntelliJ IDEA版本号代替localPath
    plugins.set(listOf("java")) // 添加Java插件依赖
    updateSinceUntilBuild = false // 关闭版本检查
}

tasks {
    runIde {
        jvmArgs("-Dkotlinx.coroutines.javaagent.enabled=false")
    }

    buildPlugin {
        archiveBaseName.set("DeadLockDetectorPlugin")
        archiveVersion.set("1.0.0") // 插件版本写在这里
    }
}