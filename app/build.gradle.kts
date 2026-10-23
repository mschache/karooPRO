import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

// Single source of truth for the version. CI checks that a release tag `vX.Y.Z` matches it.
val appVersionName: String = providers.gradleProperty("karoopro.version").get()
val appVersionCode: Int = appVersionName.split(".").map { it.toInt() }.let { (major, minor, patch) ->
    major * 10_000 + minor * 100 + patch
}
val releaseBaseUrl = "https://github.com/mschache/karooPRO/releases/latest/download"

// Release signing comes from env vars (CI) or an untracked keystore.properties (local).
val signingProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String): String? = System.getenv(key) ?: signingProps.getProperty(key)

android {
    namespace = "io.github.mschache.karoopro"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.mschache.karoopro"
        minSdk = 26
        targetSdk = 34
        versionCode = appVersionCode
        versionName = appVersionName
        manifestPlaceholders["manifestUrl"] = "$releaseBaseUrl/manifest.json"
    }

    signingConfigs {
        create("release") {
            val storePath = signingValue("KEYSTORE_FILE")
            val base64 = signingValue("KEYSTORE_BASE64")
            storeFile = when {
                storePath != null -> rootProject.file(storePath)
                base64 != null -> File.createTempFile("karoopro", ".jks").apply {
                    writeBytes(Base64.getDecoder().decode(base64))
                    deleteOnExit()
                }
                else -> null
            }
            storePassword = signingValue("KEYSTORE_PASSWORD")
            keyAlias = signingValue("KEY_ALIAS")
            keyPassword = signingValue("KEY_PASSWORD")
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ""
        }
        release {
            isMinifyEnabled = false
            val release = signingConfigs.getByName("release")
            if (release.storeFile != null) signingConfig = release
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        disable += setOf("GradleDependency", "NewerVersionAvailable", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}

tasks.register("generateManifest") {
    description = "Writes app/manifest.json (KarooAppManifest) for the GitHub release"
    group = "build"
    val out = layout.projectDirectory.file("manifest.json")
    val releaseNotes = providers.gradleProperty("karoopro.releaseNotes").orElse("")
    inputs.property("version", appVersionName)
    inputs.property("releaseNotes", releaseNotes)
    outputs.file(out)
    doLast {
        val manifest = mapOf(
            "label" to "KarooPRO",
            "packageName" to "io.github.mschache.karoopro",
            "iconUrl" to "$releaseBaseUrl/karoopro.png",
            "latestApkUrl" to "$releaseBaseUrl/app-release.apk",
            "latestVersion" to appVersionName,
            "latestVersionCode" to appVersionCode,
            "developer" to "github.com/mschache",
            "description" to "Ride by effort: one full-page field with a big zone-colored number and " +
                "Ride, Effort, Weather, Lap, Analysis, Workout and Bike panels you flip with a tap.",
            "releaseNotes" to releaseNotes.get(),
            "screenshotUrls" to listOf(
                "$releaseBaseUrl/screenshot-1.png",
                "$releaseBaseUrl/screenshot-2.png",
                "$releaseBaseUrl/screenshot-3.png",
            ),
            "tags" to listOf("performance"),
        )
        out.asFile.writeText(groovy.json.JsonBuilder(manifest).toPrettyString())
        println("manifest.json -> $appVersionName ($appVersionCode)")
    }
}

dependencies {
    implementation(libs.karoo.ext)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.bundles.lifecycle)
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.datastore.preferences)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
