import com.android.build.api.variant.BuildConfigField
import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

hilt {
    enableAggregatingTask = false
}

android {
    namespace = "com.evmap.mobileapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.evmap.mobileapp"
        minSdk = 26
        targetSdk = 35
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
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }
}

androidComponents {
    onVariants { variant ->
        val release = variant.buildType == "release"
        val property = if (release) "evmapReleaseApiUrl" else "evmapDebugApiUrl"
        val environment = if (release) "EVMAP_RELEASE_API_URL" else "EVMAP_DEBUG_API_URL"
        val url = providers.gradleProperty(property).orElse(providers.environmentVariable(environment))
        val configuredUrl = if (release) url.orElse(providers.provider {
            throw GradleException("Set -P$property or $environment to the HTTPS API URL for release builds.")
        }) else url.orElse("http://10.0.2.2:8080/")
        checkNotNull(variant.buildConfigFields).put("BASE_URL", configuredUrl.map { value ->
            val uri = runCatching { URI(value.trim()) }.getOrNull()
            val schemes = if (release) setOf("https") else setOf("http", "https")
            if (uri == null || uri.scheme !in schemes || uri.host == null || uri.userInfo != null ||
                uri.query != null || uri.fragment != null || (uri.path != "" && uri.path != "/") ||
                (uri.port != -1 && uri.port !in 1..65535)
            ) throw GradleException("$property must be an API origin (${schemes.joinToString(" or ")}).")
            BuildConfigField("String", "\"${uri.toASCIIString().trimEnd('/')}/\"", null)
        })
    }
}

dependencies {
    //implementation("com.squareup:javapoet:1.13.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.09.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)


    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.navigation:navigation-compose:2.9.3")
    implementation("androidx.activity:activity-compose:1.10.1")

    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.2")

    implementation("androidx.paging:paging-runtime-ktx:3.3.2")
    implementation("androidx.paging:paging-compose:3.3.2")
    implementation("org.maplibre.gl:android-sdk:11.13.0")

    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-android-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

}

//configurations.configureEach {
//    resolutionStrategy.eachDependency {
//        if (requested.group == "com.squareup" && requested.name == "javapoet") {
//            useVersion("1.13.0")
//        }
//    }
//resolutionStrategy.force("com.squareup:javapoet:1.13.0")
//}
