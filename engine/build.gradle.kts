// Kassi data engine — pure Kotlin, zero IntelliJ dependencies.
// This is the shared, fully-tested core reused by every plugin surface in the monorepo:
// checksums (IBAN/Luhn/VAT/BSN/ABN/TFN), per-country registries, and the generators.
plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(17)
}

tasks.test {
    useJUnitPlatform()
}
