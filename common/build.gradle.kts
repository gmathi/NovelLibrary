plugins {
    alias(libs.plugins.android.application) apply false
    id("com.android.library")
}

android {
    namespace = "io.github.gmathi.novellibrary.common"
    compileSdk = 36
    buildToolsVersion = "36.0.0"
    
    defaultConfig {
        minSdk = 23
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    flavorDimensions += "mode"
    productFlavors {
        create("mirror") {
            dimension = "mode"
        }
        create("canary") {
            dimension = "mode"
        }
        create("normal") {
            dimension = "mode"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    // Material Design
    implementation(libs.material)
    
    // AndroidX
    implementation(libs.androidx.appcompat)
    
    // Testing
    testImplementation(libs.junit)
    testImplementation("io.kotest:kotest-runner-junit5:5.9.1")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("io.kotest:kotest-property:5.9.1")
    testImplementation("io.mockk:mockk:1.13.13")
    
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso)
}
