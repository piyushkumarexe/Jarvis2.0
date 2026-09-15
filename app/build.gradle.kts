plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "ai.jarvis.assistant"; compileSdk = 35
    defaultConfig { applicationId = "ai.jarvis.assistant"; minSdk = 26; targetSdk = 35; versionCode = 2; versionName = "1.1" }
    val nvidiaKey = (project.findProperty("NVIDIA_API_KEY") as String? ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
    signingConfigs { val signingPath = System.getenv("JARVIS_KEYSTORE_PATH"); if (!signingPath.isNullOrBlank()) { create("jarvis") { storeFile = file(signingPath); storePassword = System.getenv("JARVIS_KEYSTORE_PASSWORD"); keyAlias = System.getenv("JARVIS_KEY_ALIAS") ?: "jarvis"; keyPassword = System.getenv("JARVIS_KEY_PASSWORD"); storeType = "pkcs12" } } }
    buildTypes { getByName("debug") { if (!System.getenv("JARVIS_KEYSTORE_PATH").isNullOrBlank()) signingConfig = signingConfigs.getByName("jarvis"); buildConfigField("String", "NVIDIA_API_KEY", "\"$nvidiaKey\""); buildConfigField("String", "NVIDIA_BASE_URL", "\"https://integrate.api.nvidia.com/v1\""); buildConfigField("String", "NVIDIA_MODEL", "\"nvidia/nemotron-3-ultra-550b-a55b\"") } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
