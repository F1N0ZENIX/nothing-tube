
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.f1n0.nothingtube"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.f1n0.nothingtube"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    jvmToolchain(17)
}


// The uploaded brand logo lives in the repository root at assets/.
// Copy it into Android resources and create a square, padded launcher version before resource processing.
tasks.register("prepareCustomLogo") {
    doLast {
        val source = rootProject.file("assets/1791650972965.jpg")
        if (!source.exists()) {
            throw GradleException("Logo não encontrada: ${source.absolutePath}")
        }
        val drawableDir = layout.projectDirectory.dir("src/main/res/drawable-nodpi").asFile
        drawableDir.mkdirs()
        source.copyTo(drawableDir.resolve("custom_logo.jpg"), overwrite = true)
        source.copyTo(drawableDir.resolve("ic_launcher_custom.jpg"), overwrite = true)
    }
}

tasks.named("preBuild").configure {
    dependsOn("prepareCustomLogo")
}
