// Root of the Kassi DevTools monorepo.
// One shared, fully-tested data engine (:engine) + one module per JetBrains plugin (:plugins:*).
plugins {
    kotlin("jvm") version "1.9.23" apply false
    id("org.jetbrains.intellij") version "1.17.4" apply false
}

allprojects {
    group = "io.github.vadimtoptunov"
    repositories { mavenCentral() }
}
