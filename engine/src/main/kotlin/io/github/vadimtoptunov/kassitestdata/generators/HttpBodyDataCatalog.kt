package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry

/**
 * Curated subset of the shared engine for pasting straight into an HTTP request: raw
 * (unformatted) IBAN/PAN/BIC values, plus a coherent persona as a JSON object. Unlike
 * [TestDataCatalog] (built for human-readable insertion at a caret in any file), every value
 * here is either a bare token or valid JSON, so it drops directly into a `.http` request body
 * or header without further editing.
 */
object HttpBodyDataCatalog {

    fun items(): List<CatalogItem> {
        val items = mutableListOf<CatalogItem>()

        for (country in IbanRegistry.supportedCountries) {
            items += CatalogItem("IBAN", "IBAN (${country.code}) · valid · ${country.displayName}") { seed ->
                BankAccountGenerator.iban(country, Rng(seed), valid = true)
            }
            items += CatalogItem("IBAN", "IBAN (${country.code}) · invalid · ${country.displayName}") { seed ->
                BankAccountGenerator.iban(country, Rng(seed), valid = false)
            }
        }

        for (network in CardGenerator.Network.entries) {
            items += CatalogItem("Card", "PAN · ${network.displayName} · valid") { seed ->
                CardGenerator.pan(network, Rng(seed), valid = true)
            }
            items += CatalogItem("Card", "PAN · ${network.displayName} · invalid (fails Luhn)") { seed ->
                CardGenerator.pan(network, Rng(seed), valid = false)
            }
        }

        for (country in Country.entries.sortedBy { it.displayName }) {
            items += CatalogItem("BIC", "BIC (${country.code}) · ${country.displayName}") { seed ->
                BicGenerator.bic(country, Rng(seed))
            }
        }

        for (country in Country.entries.sortedBy { it.displayName }) {
            items += CatalogItem("Persona", "Persona JSON (${country.code}) · ${country.displayName}") { seed ->
                personaJson(country, seed)
            }
        }

        return items
    }

    private fun personaJson(country: Country, seed: Long?): String {
        val persona = PersonaGenerator.generate(country, seed)
        return buildString {
            appendLine("{")
            appendLine("  \"fullName\": \"${persona.fullName.jsonEscaped()}\",")
            appendLine("  \"dateOfBirth\": \"${persona.dateOfBirth}\",")
            appendLine("  \"country\": \"${persona.country.code}\",")
            appendLine("  \"address\": \"${persona.address.jsonEscaped()}\",")
            appendLine("  \"bankAccount\": \"${persona.bankValue.jsonEscaped()}\",")
            append("  \"bic\": \"${persona.bic}\"")
            if (persona.nationalId != null) append(",\n  \"nationalId\": \"${persona.nationalId}\"")
            if (persona.taxId != null) append(",\n  \"taxId\": \"${persona.taxId}\"")
            append("\n}")
        }
    }

    private fun String.jsonEscaped(): String = replace("\\", "\\\\").replace("\"", "\\\"")
}
