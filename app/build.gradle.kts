import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.serialization)
}

val localPropertiesFile = rootProject.file("local.properties")
val groqApiKey: String? = if (localPropertiesFile.exists()) {
    val properties = Properties()
    properties.load(localPropertiesFile.inputStream())
    properties.getProperty("GROQ_API_KEY")
} else {
    null
}

if (groqApiKey == null) {
    println("Warning: 'GROQ_API_KEY' not found in local.properties. The AI chatbot will not work.")
} else {
    println("Loaded GROQ_API_KEY from local.properties")
}

val weatherApiKey: String? = if (localPropertiesFile.exists()) {
    val properties = Properties()
    properties.load(localPropertiesFile.inputStream())
    properties.getProperty("WEATHER_API_KEY")
} else {
    null
}

if (weatherApiKey == null) {
    println("Warning: 'WEATHER_API_KEY' not found in local.properties. Weather features will not work.")
} else {
    println("Loaded WEATHER_API_KEY from local.properties")
}

val googleMapsApiKey: String? = if (localPropertiesFile.exists()) {
    val properties = Properties()
    properties.load(localPropertiesFile.inputStream())
    properties.getProperty("GOOGLE_MAPS_API_KEY")
} else {
    null
}

if (googleMapsApiKey == null) {
    println("Warning: 'GOOGLE_MAPS_API_KEY' not found in local.properties. Maps features will not work.")
} else {
    println("Loaded GOOGLE_MAPS_API_KEY from local.properties")
}

android {
    namespace = "com.tharun.vitalmind"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.tharun.vitalmind"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GROQ_API_KEY", "\"${groqApiKey ?: ""}\"")
        buildConfigField("String", "WEATHER_API_KEY", "\"${weatherApiKey ?: ""}\"")
        buildConfigField("String", "GOOGLE_MAPS_API_KEY", "\"${googleMapsApiKey ?: ""}\"")

        // Manifest placeholders for API keys
        manifestPlaceholders["GOOGLE_MAPS_API_KEY"] = googleMapsApiKey ?: ""
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

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions { jvmTarget = "1.8" }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
}

dependencies {

    // Google Fit
    implementation("com.google.android.gms:play-services-fitness:21.3.0")
    implementation("com.google.android.gms:play-services-auth:21.4.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Google Maps
    implementation("com.google.maps.android:maps-compose:4.3.0")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // AndroidX + Compose
    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.6.1")

    // ✅ REQUIRED FIX (Navigation Compose)
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Charts
    implementation("com.patrykandpatrick.vico:compose-m3:1.14.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.4.0")

    // Ktor Client
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Play Services Location
    implementation("com.google.android.gms:play-services-location:21.3.0")
}

configurations.all {
    resolutionStrategy.eachDependency {
        if (requested.group == "org.jetbrains.kotlinx" && requested.name.startsWith("kotlinx-serialization")) {
            useVersion("1.6.3")
        }
    }
}
