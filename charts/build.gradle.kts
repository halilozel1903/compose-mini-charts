plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

android {
    namespace = "io.github.halilozel1903.minicharts"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    explicitApi()
}

dependencies {
    api(project(":charts-core"))

    implementation(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.compose-mini-charts".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "compose-mini-charts")
    pom {
        name.set("Compose Mini Charts")
        description.set("Lightweight charts for Jetpack Compose: sparkline, line, bar and donut with animations, touch highlight and accessibility. Canvas only, no dependencies.")
    }
}
