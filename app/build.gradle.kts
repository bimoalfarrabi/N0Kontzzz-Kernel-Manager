plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
}

configure <com.android.build.api.dsl.ApplicationExtension> {
    namespace = "id.nkz.nokontzzzmanager"
    compileSdk = 36

    defaultConfig {
        applicationId = "id.nkz.nokontzzzmanager"
        minSdk = 26
        targetSdk = 36
        versionCode = 74
        versionName = "1.5.6"
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint {
        disable.add("NullSafeMutableLiveData")
    }
    buildFeatures { compose = true }
}

configurations.all {
    resolutionStrategy {
        force(libs.guava)
    }
    exclude(group = "com.google.guava", module = "listenablefuture")
}

kotlin {
    compilerOptions { 
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) 
    }
}

dependencies {
    // Core & App
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    // Navigation & Lifecycle
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt (Dependency Injection)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.hilt.android.testing)

    // Data
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.collections.immutable)

    // Background Tasks
    implementation(libs.androidx.work.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Utility
    implementation(libs.libsu)
    implementation(libs.coil.compose)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.guava) {
        exclude(group = "com.google.guava", module = "listenablefuture")
    }

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.mockito.core)
    testImplementation(libs.robolectric)
}

configurations.all {
    resolutionStrategy {
        force("com.google.guava:guava:33.5.0-android")
        exclude(group = "com.google.guava", module = "listenablefuture")
    }
}
