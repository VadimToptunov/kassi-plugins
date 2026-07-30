// Kassi HTTP Data Injector — insert valid synthetic IBAN/PAN/BIC/Persona data into .http files.
plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

// Overridable at release time: -PhttpDataInjectorVersion=1.0.1
version = (findProperty("pluginVersion") as String?) ?: "1.0.0"

intellij {
    version.set("2023.2.6")
    type.set("IC") // IntelliJ IDEA Community; platform-only, so it also loads in Android Studio.
    plugins.set(listOf())
}

dependencies {
    implementation(project(":engine")) // the shared, tested data engine is bundled into the plugin
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    test {
        useJUnitPlatform()
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
