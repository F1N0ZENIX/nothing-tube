
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
        val screenLogo = drawableDir.resolve("custom_logo.jpg")
        source.copyTo(screenLogo, overwrite = true)

        val image = javax.imageio.ImageIO.read(source)
            ?: throw GradleException("Não foi possível ler a imagem da logo.")
        val canvasSize = 512
        val canvas = java.awt.image.BufferedImage(
            canvasSize, canvasSize, java.awt.image.BufferedImage.TYPE_INT_RGB
        )
        val graphics = canvas.createGraphics()
        graphics.color = java.awt.Color.BLACK
        graphics.fillRect(0, 0, canvasSize, canvasSize)
        graphics.setRenderingHint(
            java.awt.RenderingHints.KEY_INTERPOLATION,
            java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC
        )
        graphics.setRenderingHint(
            java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON
        )
        val scale = minOf(420.0 / image.width, 420.0 / image.height)
        val width = (image.width * scale).toInt().coerceAtLeast(1)
        val height = (image.height * scale).toInt().coerceAtLeast(1)
        graphics.drawImage(image, (canvasSize - width) / 2, (canvasSize - height) / 2, width, height, null)
        graphics.dispose()
        javax.imageio.ImageIO.write(canvas, "png", drawableDir.resolve("ic_launcher_custom.png"))
    }
}

tasks.named("preBuild").configure {
    dependsOn("prepareCustomLogo")
}
