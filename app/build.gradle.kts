plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.xjyzs.speedviewer"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.xjyzs.speedviewer"
        minSdk = 26
        targetSdk = 37
        versionCode = 3
        versionName = "1.1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        androidResources.localeFilters += listOf("zh")
    }
    signingConfigs {
        create("release") {
            storeFile = file("${project.rootDir}/keystore.jks")
            storePassword = System.getenv("KEY_STORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            enableV1Signing = false
        }
    }

    flavorDimensions += "abi"
    productFlavors {
        val signingConfig = if (signingConfigs.findByName("release") != null) {
            signingConfigs.getByName("release")
        } else {
            signingConfigs.getByName("debug")
        }
        create("x86") {
            dimension = "abi"
            ndk { abiFilters.add("x86") }
            this.signingConfig = signingConfig
        }
        create("x86_64") {
            dimension = "abi"
            ndk { abiFilters.add("x86_64") }
            this.signingConfig = signingConfig
        }
        create("arm") {
            dimension = "abi"
            ndk { abiFilters.add("armeabi-v7a") }
            this.signingConfig = signingConfig
        }
        create("arm64Minsdk35") {
            dimension = "abi"
            ndk { abiFilters.add("arm64-v8a") }
            minSdk = 35
            this.signingConfig = signingConfig
        }
        create("arm64Minsdk29") {
            dimension = "abi"
            ndk { abiFilters.add("arm64-v8a") }
            minSdk = 29
            this.signingConfig = signingConfig
        }
        create("universal") {
            dimension = "abi"
            this.signingConfig = signingConfig
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro"
            )
            packaging {
                resources {
                    excludes += setOf(
                        "DebugProbesKt.bin",
                        "kotlin-tooling-metadata.json",
                        "META-INF/**",
                        "kotlin/**"
                    )
                }
            }
            tasks.configureEach {
                doLast {
                    outputs.files.forEach { outputDir ->
                        val filesToDelete = setOf("PublicSuffixDatabase.list")
                        for (i in filesToDelete) {
                            val file = outputDir.resolve(i)
                            if (file.exists()) {
                                file.delete()
                            }
                        }
                    }
                }
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        aidl = true
    }
}

dependencies {
    // implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    // implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.compose)
}