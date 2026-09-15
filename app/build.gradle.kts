plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android { namespace = "ai.jarvis.assistant"; compileSdk = 35
    defaultConfig { applicationId = "ai.jarvis.assistant"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "1.0" }
    val nvidiaKey = (project.findProperty("NVIDIA_API_KEY") as String? ?: "").replace("\\", "\\\\").replace("\"", "\\\"")
    buildTypes { getByName("debug") { buildConfigField("String", "NVIDIA_API_KEY", "\"$nvidiaKey\""); buildConfigField("String", "NVIDIA_BASE_URL", "\"https://integrate.api.nvidia.com/v1\""); buildConfigField("String", "NVIDIA_MODEL", "\"nvidia/nemotron-3-ultra-550b-a55b\"") } }
    buildFeatures { compose = true; buildConfig = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
dependencies {
    implementation(composeBom)
    androidTestImplementation(composeBom)
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
