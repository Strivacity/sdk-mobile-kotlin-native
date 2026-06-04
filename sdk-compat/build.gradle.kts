plugins { alias(libs.plugins.android.library)
  alias(libs.plugins.kotlin.android)
}

android {
  namespace = "com.strivacity.android.native_sdk.compat"
  compileSdk = 36

  defaultConfig {
    minSdk = 29

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
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
  kotlinOptions {
    jvmTarget = "11"
  }
}

dependencies {
  val isPublishing = gradle.startParameter.taskNames.any { it.contains("publish", ignoreCase = true) }
  if (isPublishing) {
    // >= 3.3.3 <4.0.0
    api("com.strivacity.android:kotlin_native_sdk") {
      version {
        require("[3.3.0, 4.0.0)")
      }
    }
  } else {
    api(project(":sdk"))
  }
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(libs.core.ktx)
  implementation(libs.kotlinx.coroutines.jdk9)
  implementation(libs.androidx.lifecycle.livedata.ktx)
}
