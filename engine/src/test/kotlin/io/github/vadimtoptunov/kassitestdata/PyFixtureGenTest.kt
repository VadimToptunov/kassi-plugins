package io.github.vadimtoptunov.kassitestdata

import io.github.vadimtoptunov.kassitestdata.codegen.GoTableTestGen
import io.github.vadimtoptunov.kassitestdata.codegen.PyFixtureGen
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PyFixtureGenTest {

    @Test
    fun `emits a pytest parametrized test with one row per engine case`() {
        val py = PyFixtureGen.generate(GoTableTestGen.Kind.IBAN)
        assertTrue(py.startsWith("import pytest"), py)
        assertTrue(py.contains("@pytest.mark.parametrize("))
        assertTrue(py.contains("def test_validate_iban(iban, valid):"))
        assertTrue(py.contains("assert validate_iban(iban) is valid"))
        // one parametrize row per case, valid booleans rendered as Python True/False
        val rows = py.lines().count { it.trimStart().startsWith("(") && it.contains("),  #") }
        assertEquals(GoTableTestGen.cases(GoTableTestGen.Kind.IBAN).size, rows)
        assertTrue(py.contains(", True),") && py.contains(", False),"))
    }

    @Test
    fun `is deterministic under a seed`() {
        assertEquals(
            PyFixtureGen.generate(GoTableTestGen.Kind.CARD, 7L),
            PyFixtureGen.generate(GoTableTestGen.Kind.CARD, 7L),
        )
    }
}
