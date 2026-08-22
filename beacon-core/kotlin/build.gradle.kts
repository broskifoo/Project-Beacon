plugins {
    kotlin("multiplatform") version "2.0.0"
    id("org.jetbrains.kotlinx.serialization") version "2.0.0"
    id("com.android.library") version "8.4.0"
}

group = "org.beacon.core"
version = "0.1.0-alpha"

kotlin {
    androidTarget {
        publishLibraryVariants("release")
    }
    jvm()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib-common"))
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation(project(":beacon-sdk:kotlin"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation("androidx.core:core-ktx:1.12.0")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
                implementation("androidx.datastore:datastore-preferences:1.1.1")
                implementation("org.sqlcipher:sqlcipher:4.5.3")
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("stdlib-jdk8"))
                implementation("org.sqlcipher:sqlcipher:4.5.3")
            }
        }
    }
}

android {
    namespace = "org.beacon.core"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-alpha"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlinx.serialization.ExperimentalSerializationApi"
        )
    }
}