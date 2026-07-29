package io.github.vadimtoptunov.httpdatainjector

import com.intellij.testFramework.LightVirtualFile
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The plugin-surface-only piece worth a unit test: which files the intention/action activate in. */
class HttpDataInjectorFileGatingTest {

    @Test
    fun `http files are recognised case-insensitively`() {
        assertTrue(HttpDataInjector.isHttpFile(LightVirtualFile("requests.http")))
        assertTrue(HttpDataInjector.isHttpFile(LightVirtualFile("requests.HTTP")))
    }

    @Test
    fun `non-http files and no file are rejected`() {
        assertFalse(HttpDataInjector.isHttpFile(LightVirtualFile("requests.json")))
        assertFalse(HttpDataInjector.isHttpFile(LightVirtualFile("Notes.txt")))
        assertFalse(HttpDataInjector.isHttpFile(null))
    }
}
