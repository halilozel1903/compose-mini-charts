import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.maven.publish)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    explicitApi()
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    testImplementation(libs.kotlin.test)
}

mavenPublishing {
    publishToMavenCentral()
    // Sign only when a key is configured (Maven Central); JitPack and local builds stay unsigned.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    // JitPack serves artifacts under com.github.<user>.<repo>.
    val jitpackGroup = "com.github.halilozel1903.compose-mini-charts".takeIf { System.getenv("JITPACK") == "true" }
    coordinates(groupId = jitpackGroup, artifactId = "compose-mini-charts-core")
    pom {
        name.set("Compose Mini Charts Core")
        description.set("Platform independent chart math behind compose-mini-charts: nice axis ticks, scaling, line smoothing, bar and donut layout, hit testing, formatting and accessibility summaries.")
    }
}
