plugins { alias(libs.plugins.android.application) }

android {
  namespace = "com.strivacity.android.native_sdk.javademo"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.strivacity.android.native_sdk.javademo"
    minSdk = 29
    targetSdk = 36
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures { viewBinding = true }
}

dependencies {
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.androidx.annotation)
  implementation(libs.androidx.constraintlayout)
  implementation(libs.androidx.lifecycle.livedata.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.browser.browser)
  implementation(libs.kotlinx.coroutines.jdk9)

  implementation(project(":sdk-compat"))
  implementation(libs.androidx.credentials)
}
