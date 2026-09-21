// Secret & Card Leak Guard — inspection that flags real-looking card numbers, IBANs and US SSNs
// (checksum-valid, outside every known reserved-for-testing range) with a synthetic-replacement fix.
plugins {
    id("java")
    kotlin("jvm")
    id("org.jetbrains.intellij")
}

// Overridable at release time: -PleakGuardVersion=1.0.1
version = (findProperty("pluginVersion") as String?) ?: "1.2.0"

intellij {
    version.set("2023.2.6")
    type.set("IC") // IntelliJ IDEA Community; platform-only, so it also loads in Android Studio.
    plugins.set(listOf())
}

dependencies {
    implementation(project(":engine")) // the shared, tested data engine is bundled into the plugin
    testImplementation("junit:junit:4.13.2") // BasePlatformTestCase is a JUnit4-run platform test
}

kotlin {
    jvmToolchain(17)
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }

    // Headless CLI / CI entry point: scans files for real-looking secrets and card/IBAN/SSN values
    // without an IDE, with SARIF output and a line-independent baseline (fail only on NEW findings).
    //   ./gradlew :plugins:leak-guard:runCli --args="--sarif leaks.sarif --baseline lg-baseline.json src"
    register<JavaExec>("runCli") {
        group = "application"
        description = "Runs Leak Guard on files/dirs without IntelliJ (SARIF + baseline)."
        mainClass.set("io.github.vadimtoptunov.leakguard.cli.CliKt")
        classpath = sourceSets["main"].runtimeClasspath
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
