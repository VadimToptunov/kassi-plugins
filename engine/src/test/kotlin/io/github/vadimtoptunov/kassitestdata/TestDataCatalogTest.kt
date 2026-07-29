package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.generators.TestDataCatalog
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TestDataCatalogTest {

    @Test
    fun `every catalog item produces non-empty output, seeded and unseeded, without error`() {
        val items = TestDataCatalog.allItems()
        assertTrue(items.size > 100, "expected broad coverage, got ${items.size} items")
        for (item in items) {
            val seeded = item.produce(42L)
            val unseeded = item.produce(null)
            assertFalse(seeded.isBlank(), "blank output for ${item.label}")
            assertFalse(unseeded.isBlank(), "blank output for ${item.label}")
        }
    }

    @Test
    fun `seeded catalog output is reproducible`() {
        for (item in TestDataCatalog.allItems()) {
            assertTrue(item.produce(7L) == item.produce(7L), "not reproducible: ${item.label}")
        }
    }
}
