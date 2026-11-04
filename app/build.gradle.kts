import java.io.File
import java.net.URI
import java.util.Base64

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.wisdomtower.academy"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.wisdomtower.academy"
    minSdk = 24
    targetSdk = 36
    versionCode = 3
    versionName = "1.0.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    // TELEGRAM BUILD SWITCH (owner order 2026-10-23, docs/TELEGRAM_PAID_BUILD_PLAN.md): false for the Play Store build,
    // true only when Gradle runs with -PpaidUi=true (the CI step "Build Telegram APK"). Paid screens read BuildConfig.PAID_UI.
    buildConfigField("boolean", "PAID_UI", (project.findProperty("paidUi") == "true").toString())
  }

  signingConfigs {
    create("release") {
      val base64Key = System.getenv("KEYSTORE_BASE64")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: if (!base64Key.isNullOrBlank()) {
        val decodedFile = file("${rootDir}/my-upload-key.jks")
        if (!decodedFile.exists() || decodedFile.length() == 0L) {
          decodedFile.writeBytes(Base64.getDecoder().decode(base64Key.trim()))
        }
        decodedFile.absolutePath
      } else {
        "${rootDir}/my-upload-key.jks"
      }
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
      keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("STORE_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseStore = System.getenv("KEYSTORE_PATH")
      val hasKeystore = (!releaseStore.isNullOrBlank() && file(releaseStore).exists()) ||
          (!System.getenv("KEYSTORE_BASE64").isNullOrBlank()) ||
          file("${rootDir}/my-upload-key.jks").exists()
      val hasReleaseEnv = hasKeystore && !System.getenv("STORE_PASSWORD").isNullOrBlank()
      signingConfig = if (hasReleaseEnv) {
        signingConfigs.getByName("release")
      } else if (System.getenv("CI") == "true") {
        throw GradleException("Production release build requires valid upload keystore secrets (KEYSTORE_BASE64/KEYSTORE_PATH and STORE_PASSWORD) in CI.")
      } else {
        logger.warn("WARNING: using fallback keystore — NOT for Play Store or public Telegram production")
        signingConfigs.getByName("debugConfig")
      }
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

tasks.register<Copy>("copyBrandAssets") {
  from(rootDir) {
    include("logo.png", "animation.gif")
  }
  into(layout.projectDirectory.dir("src/main/assets/brand"))
}
tasks.named("preBuild").configure { dependsOn("copyBrandAssets") }

// Gives the local web pages (notes, flashcards, quizzes in src/main/assets/notes/) the SAME fonts as the native
// screens (Outfit and Plus Jakarta Sans, already packed in src/main/res/font for Compose). The chat tools cannot
// commit binary files, so every build copies the .ttf files next to the pages: assets/notes/fonts/*.ttf, which
// `richtext.css` loads with @font-face (owner problem 5, 2026-10-12). If the folder is missing nothing is
// copied and the build still succeeds (the pages then fall back to the system font).
tasks.register<Copy>("copyWebFonts") {
  from(layout.projectDirectory.dir("src/main/res/font")) {
    include("plus_jakarta_sans_*.ttf", "outfit_*.ttf")
  }
  into(layout.projectDirectory.dir("src/main/assets/notes/fonts"))
}
tasks.named("preBuild").configure { dependsOn("copyWebFonts") }

// Packs the images the website added (Grade 9-12 subject photos, Entrance Questions pictures, Remedial covers) from the
// synced website-reference snapshot, so they stay packed in the app and follow every "Sync website reference".
// Result: src/main/assets/images/web/grade-{9..12}/*, images/web/entrance/{natural,social,entrance-questions}.jpeg,
// images/web/remedial/{english,maths,physics,chemistry,biology,history,geography}.jpg
// (the folder names with spaces on the website are renamed here so the asset paths have no spaces).
// If the source folder is missing the task copies nothing and the build still succeeds.
tasks.register<Copy>("copyWebsiteImages") {
  val webImages = rootDir.resolve("website-reference/public/images")
  into(layout.projectDirectory.dir("src/main/assets/images/web"))
  listOf("9", "10", "11", "12").forEach { g ->
    from(webImages.resolve("Grade $g")) {
      into("grade-$g")
      exclude(".gitkeep")
    }
  }
  from(webImages) {
    include("natural.jpeg", "social.jpeg")
    into("entrance")
  }
  from(webImages) {
    include("Entrance questions.jpeg")
    rename("Entrance questions.jpeg", "entrance-questions.jpeg")
    into("entrance")
  }
  // UAT hub artwork: SVG placeholders are the official fallback until 16:9 JPGs are uploaded.
  from(webImages.resolve("uat-hubs")) {
    include("*.svg", "*.jpg", "*.jpeg")
    into("uat-hubs")
  }
  // Remedial covers: the files in website-reference/public/images/Remedial/ are named remedial-{subject}.jpg
  // ("Remedial-physics.jpg" has a capital R, "remedial-math.jpg" is the Maths cover). They are renamed to the
  // website subject ids (english, maths, physics, ...) so the app can look them up by id.
  mapOf(
    "remedial-english.jpg" to "english.jpg",
    "remedial-math.jpg" to "maths.jpg",
    "Remedial-physics.jpg" to "physics.jpg",
    "remedial-chemistry.jpg" to "chemistry.jpg",
    "remedial-biology.jpg" to "biology.jpg",
    "remedial-history.jpg" to "history.jpg",
    "remedial-geography.jpg" to "geography.jpg"
  ).forEach { (source, target) ->
    from(webImages.resolve("Remedial")) {
      include(source)
      rename(source, target)
      into("remedial")
    }
  }
}
tasks.named("preBuild").configure { dependsOn("copyWebsiteImages") }

// Keeps the ECE / special-package pictures (home card ece.jpg, semester covers, course covers) in step with the
// synced website snapshot. Same folder and file names as the website, so the paths already used by the app
// (file:///android_asset/images/special-packages/...) now show the website's current pictures. A file that is
// missing on the website keeps the picture already stored in the app.
tasks.register<Copy>("copySpecialPackageImages") {
  from(rootDir.resolve("website-reference/public/images/special-packages")) {
    exclude(".gitkeep", "README.md")
  }
  into(layout.projectDirectory.dir("src/main/assets/images/special-packages"))
}
tasks.named("preBuild").configure { dependsOn("copySpecialPackageImages") }

// Packs KaTeX (the website's math library, same major version 0.16) for the local notes page
// (src/main/assets/notes/index.html, plan step N6). The files cannot be committed from the chat tools, so every
// build downloads the pinned version from the jsDelivr npm mirror into src/main/assets/notes/katex/ and keeps
// them (a file that is already there is not downloaded again). If a download fails the build still succeeds
// with a warning: the notes page then shows formulas as plain text instead of typeset math.
// NOTE (build fix 2026-10-11): inside a Gradle Kotlin script the bare name `java` means the project's `java`
// extension, so `java.io.File` / `java.net.URI` did NOT resolve ("Unresolved reference 'io'"). Use the imports
// at the top of this file (`File`, `URI`) and never write a fully qualified `java.xxx` name in this file.
tasks.register("fetchKatex") {
  val katexVersion = "0.16.11"
  val outDir = layout.projectDirectory.dir("src/main/assets/notes/katex").asFile
  val fontNames = listOf(
    "AMS-Regular", "Caligraphic-Bold", "Caligraphic-Regular", "Fraktur-Bold", "Fraktur-Regular",
    "Main-Bold", "Main-BoldItalic", "Main-Italic", "Main-Regular", "Math-BoldItalic", "Math-Italic",
    "SansSerif-Bold", "SansSerif-Italic", "SansSerif-Regular", "Script-Regular",
    "Size1-Regular", "Size2-Regular", "Size3-Regular", "Size4-Regular", "Typewriter-Regular"
  )
  doLast {
    val base = "https://cdn.jsdelivr.net/npm/katex@$katexVersion/dist"
    val names = listOf("katex.min.js", "katex.min.css") + fontNames.map { "fonts/KaTeX_$it.woff2" }
    var failed = 0
    for (name in names) {
      val target = File(outDir, name)
      if (target.exists() && target.length() > 0L) continue
      try {
        target.parentFile.mkdirs()
        val connection = URI("$base/$name").toURL().openConnection()
        connection.connectTimeout = 15000
        connection.readTimeout = 30000
        connection.getInputStream().use { input -> target.outputStream().use { out -> input.copyTo(out) } }
      } catch (e: Exception) {
        target.delete()
        failed += 1
        logger.warn("fetchKatex: could not download $name (${e.javaClass.simpleName})")
      }
    }
    if (failed > 0) logger.warn("fetchKatex: $failed KaTeX file(s) missing, formulas in notes will show as plain text")
  }
}
tasks.named("preBuild").configure { dependsOn("fetchKatex") }

tasks.register<Copy>("copyApkOutputs") {
  from(layout.buildDirectory.dir("outputs/apk/debug"))
  into(rootDir.resolve("build/outputs/apk/debug"))
}
tasks.register<Copy>("copyBuildOutputs") {
  from(layout.buildDirectory.dir("outputs/apk/debug"))
  into(rootDir.resolve(".build-outputs"))
}
tasks.matching { it.name == "assembleDebug" }.configureEach {
  finalizedBy("copyApkOutputs", "copyBuildOutputs")
}

secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("KEYSTORE_BASE64")
  ignoreList.add("KEYSTORE_PATH")
  ignoreList.add("KEY_ALIAS")
  ignoreList.add("KEY_PASSWORD")
  ignoreList.add("STORE_PASSWORD")
}

dependencies {
  implementation("androidx.webkit:webkit:1.12.1")
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation("io.coil-kt:coil-gif:2.7.0")
    implementation("io.coil-kt:coil-svg:2.7.0")
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(platform(libs.firebase.bom))
  implementation(libs.firebase.messaging)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
