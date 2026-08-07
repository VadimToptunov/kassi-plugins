// UUID, ULID & NanoID Toolkit — generate and inspect these IDs offline. Thin surface over :engine.
plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

// Overridable at release time: -PpluginVersion=1.0.1
version = (findProperty("pluginVersion") as String?) ?: "1.0.0"

intellij {
    version.set("2023.2.6")
    type.set("IC") // platform-only, so it also loads in Android Studio and every IntelliJ-based IDE.
    plugins.set(listOf())
}

dependencies {
    implementation(project(":engine")) // generation/inspection logic lives in the shared, tested engine
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    patchPluginXml {
        version.set(project.version.toString())
        sinceBuild.set("232")
        untilBuild.set("")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
