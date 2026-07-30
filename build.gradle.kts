// Root of the Kassi Plugins repo.
// One shared, fully-tested data engine (:engine) + one module per JetBrains plugin (:plugins:*).
import org.jetbrains.intellij.tasks.RunPluginVerifierTask
import org.jetbrains.intellij.tasks.RunPluginVerifierTask.FailureLevel

plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("org.jetbrains.intellij") version "1.17.4" apply false
}

allprojects {
    group = "io.github.vadimtoptunov"
    repositories { mavenCentral() }
}

// Release gate for every plugin module: fail before publish on API scheduled for removal / incompatible.
subprojects {
    plugins.withId("org.jetbrains.intellij") {
        tasks.named<RunPluginVerifierTask>("runPluginVerifier") {
            ideVersions.set(listOf("IC-232.10335.12", "IC-252.28539.13"))
            failureLevel.set(
                listOf(
                    FailureLevel.COMPATIBILITY_PROBLEMS,
                    FailureLevel.INVALID_PLUGIN,
                    FailureLevel.SCHEDULED_FOR_REMOVAL_API_USAGES,
                )
            )
        }
    }
}
