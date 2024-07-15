plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    namespace = "com.nagopy.android.overlayviewmanager.opt.timber"
    compileSdk = 34

    defaultConfig {
        minSdk = 24

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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions.unitTests.isReturnDefaultValues = true
    testOptions.unitTests.isIncludeAndroidResources = true

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation("androidx.annotation:annotation:1.8.0")
    compileOnly("com.jakewharton.timber:timber:4.7.1")
    testImplementation("com.jakewharton.timber:timber:4.7.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.12.0")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}


publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.nagopy.android"
            artifactId = "overlayviewmanager-opt-timber"
            version = rootProject.ext.get("publish_version_name") as String
            afterEvaluate {
                from(components["release"])
            }

            pom {
                url = "https://github.com/75py/Android-OverlayViewManager"

                licenses {
                    license {
                        name = "Apache License Version 2.0"
                    }
                }
            }
        }
    }
}
