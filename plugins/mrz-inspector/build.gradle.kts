// MRZ & Barcode Inspector — offline live parser/validator for travel-document MRZ (TD1/TD2/TD3)
// and AAMVA PDF417 driver's-license payloads, over the shared :engine.
plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

// Overridable at release time: -PmrzInspectorVersion=1.0.1
version = (findProperty("pluginVersion") as String?) ?: "1.4.0"

intellij {
    version.set("2023.2.6")
    type.set("IC") // IntelliJ IDEA Community; platform-only, so it also loads in Android Studio.
    plugins.set(listOf())
}

dependencies {
    implementation(project(":engine")) // the shared, tested data engine is bundled into the plugin
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
