plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    `maven-publish`
    signing
}

android {
    namespace = "com.strivacity.android.native_sdk"
    compileSdk = 36

    defaultConfig {
        minSdk = 28

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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.browser)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Strivacity/sdk-mobile-kotlin-native")
            credentials {
                username = System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publishing {
        publications {
            register<MavenPublication>("release") {
                artifactId = "kotlin_native_sdk"
                groupId = "com.strivacity.android"
                // version = ""

                afterEvaluate {
                    from(components["release"])
                }

                pom {
                    name = "Strivacity Android Native SDK using Kotlin"
                    description = "Strivacity Journey-flow SDK for native clients on Android platforms using Kotlin"
                    url = "https://github.com/Strivacity/sdk-mobile-kotlin-native"

                    licenses {
                        license {
                            name = "The Apache License, Version 2.0"
                            url = "http://www.apache.org/licenses/LICENSE-2.0.txt"
                        }
                    }

                    developers {
                        developer {
                            id = "Strivacity"
                            name = "Strivacity"
                            email = "opensource@strivacity.com"
                        }
                    }

                    scm {
                        connection =
                            "scm:git:git://github.com/Strivacity/sdk-mobile-kotlin-native.git"
                        developerConnection =
                            "scm:git:ssh://github.com/Strivacity/sdk-mobile-kotlin-native.git"
                        url = "https://github.com/Strivacity/sdk-mobile-kotlin-native"
                    }
                }
            }
        }
    }
}

signing {
    sign(publishing.publications["release"])
}
