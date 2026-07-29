package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.algo.Checksums
import io.github.vadimtoptunov.kassitestdata.generators.BicGenerator
import io.github.vadimtoptunov.kassitestdata.generators.HttpBodyDataCatalog
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HttpBodyDataCatalogTest {

    private val allowedGroups = setOf("IBAN", "Card", "BIC", "Persona")

    private fun jsonField(json: String, key: String): String {
        val match = Regex("\"$key\":\\s*\"([^\"]*)\"").find(json)
            ?: error("field \"$key\" not found in:\n$json")
        return match.groupValues[1]
    }

    @Test
    fun `every item belongs to an allowed group and produces non-blank output`() {
        val items = HttpBodyDataCatalog.items()
        assertTrue(items.size > 50, "expected broad coverage, got ${items.size} items")
        for (item in items) {
            assertTrue(item.group in allowedGroups, "unexpected group ${item.group} for ${item.label}")
            assertFalse(item.produce(42L).isBlank(), "blank output for ${item.label}")
        }
    }

    @Test
    fun `IBAN items pass or fail mod-97 exactly as labelled`() {
        for (item in HttpBodyDataCatalog.items().filter { it.group == "IBAN" }) {
            val iban = item.produce(1L)
            if ("invalid" in item.label) {
                assertFalse(Checksums.isValidIbanMod97(iban), "expected invalid IBAN to fail mod-97: ${item.label} -> $iban")
            } else {
                assertTrue(Checksums.isValidIbanMod97(iban), "expected valid IBAN to pass mod-97: ${item.label} -> $iban")
            }
        }
    }

    @Test
    fun `Card items pass or fail Luhn exactly as labelled`() {
        for (item in HttpBodyDataCatalog.items().filter { it.group == "Card" }) {
            val pan = item.produce(1L)
            if ("invalid" in item.label) {
                assertFalse(Checksums.isLuhnValid(pan), "expected invalid PAN to fail Luhn: ${item.label} -> $pan")
            } else {
                assertTrue(Checksums.isLuhnValid(pan), "expected valid PAN to pass Luhn: ${item.label} -> $pan")
            }
        }
    }

    @Test
    fun `BIC items are structurally valid`() {
        for (item in HttpBodyDataCatalog.items().filter { it.group == "BIC" }) {
            val bic = item.produce(1L)
            assertTrue(BicGenerator.isStructurallyValid(bic), "expected structurally valid BIC: ${item.label} -> $bic")
        }
    }

    @Test
    fun `Persona items are valid JSON objects with a structurally valid BIC`() {
        for (item in HttpBodyDataCatalog.items().filter { it.group == "Persona" }) {
            val json = item.produce(1L)
            assertTrue(json.trim().startsWith("{") && json.trim().endsWith("}"), "not an object: ${item.label} -> $json")
            assertTrue(json.contains("\"fullName\""), "missing fullName: ${item.label}")
            assertTrue(json.contains("\"dateOfBirth\""), "missing dateOfBirth: ${item.label}")
            val bic = jsonField(json, "bic")
            assertTrue(BicGenerator.isStructurallyValid(bic), "embedded BIC not structurally valid: ${item.label} -> $bic")
        }
    }

    @Test
    fun `seeded output is reproducible`() {
        for (item in HttpBodyDataCatalog.items()) {
            assertEquals(item.produce(7L), item.produce(7L), "not reproducible: ${item.label}")
        }
    }
}
