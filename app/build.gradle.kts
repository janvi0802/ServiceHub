import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    // Reads google-services.json so the app knows which Firebase project to use
    alias(libs.plugins.google.services)
}

// Private settings from local.properties (this file is NEVER uploaded to GitHub):
//   RAZORPAY_KEY_ID=rzp_test_...            (Razorpay TEST key id)
//   ADMIN_API_URL=https://xxx.vercel.app    (the small admin server)
// If a value is missing, that feature shows "not set up" instead of crashing.
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.example.servicehub"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.servicehub"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Read in code as BuildConfig.RAZORPAY_KEY_ID and BuildConfig.ADMIN_API_URL
        buildConfigField("String", "RAZORPAY_KEY_ID", "\"${localProps.getProperty("RAZORPAY_KEY_ID", "")}\"")
        buildConfigField("String", "ADMIN_API_URL", "\"${localProps.getProperty("ADMIN_API_URL", "")}\"")
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true   // lets the app read the private settings above
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    // Firebase (versions come from the BoM)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    // Razorpay checkout (UPI / cards) in TEST mode
    implementation(libs.razorpay.checkout)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}