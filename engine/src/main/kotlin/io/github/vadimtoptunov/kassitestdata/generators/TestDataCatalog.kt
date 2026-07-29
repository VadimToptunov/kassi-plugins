package io.github.vadimtoptunov.kassitestdata.generators

import io.github.vadimtoptunov.kassitestdata.core.Country
import io.github.vadimtoptunov.kassitestdata.core.PersonaGenerator
import io.github.vadimtoptunov.kassitestdata.core.Rng
import io.github.vadimtoptunov.kassitestdata.data.IbanRegistry

/**
 * One entry in the "Insert Test Data" popup. [produce] builds a fresh [Rng] from the caller's
 * optional seed, so insertion is reproducible when a seed is configured.
 */
data class CatalogItem(
    val group: String,
    val label: String,
    val produce: (Long?) -> String,
) {
    /** Text the popup's speed-search filters against. */
    val searchText: String get() = "$group $label"
}

object TestDataCatalog {

    fun allItems(): List<CatalogItem> {
        val items = mutableListOf<CatalogItem>()

        // 1. IBAN — every IBAN country, valid + invalid.
        for (country in IbanRegistry.supportedCountries) {
            items += CatalogItem("Bank account", "IBAN (${country.code}) · valid · ${country.displayName}") { seed ->
                BankAccountGenerator.ibanFormatted(country, Rng(seed), valid = true)
            }
            items += CatalogItem("Bank account", "IBAN (${country.code}) · invalid · ${country.displayName}") { seed ->
                BankAccountGenerator.ibanFormatted(country, Rng(seed), valid = false)
            }
        }

        // Domestic bank identifiers for the non-IBAN / dual cases.
        items += CatalogItem("Bank account", "Sort code + Account (GB) · United Kingdom") { seed ->
            BankAccountGenerator.gbSortCodeAndAccount(Rng(seed))
        }
        items += CatalogItem("Bank account", "BSB + Account (AU) · Australia") { seed ->
            BankAccountGenerator.auBsbAndAccount(Rng(seed))
        }

        // 2. Card — per network, valid + invalid.
        for (network in CardGenerator.Network.entries) {
            items += CatalogItem("Card", "Card · ${network.displayName} · valid") { seed ->
                CardGenerator.card(network, Rng(seed), valid = true).formatted()
            }
            items += CatalogItem("Card", "Card · ${network.displayName} · invalid (fails Luhn)") { seed ->
                CardGenerator.card(network, Rng(seed), valid = false).formatted()
            }
        }

        // 3. BIC — every country.
        for (country in Country.entries.sortedBy { it.displayName }) {
            items += CatalogItem("BIC", "BIC (${country.code}) · ${country.displayName}") { seed ->
                BicGenerator.bic(country, Rng(seed))
            }
        }

        // 4. National ID — supported schemes (valid-only in v1; invalid variants land in v1.1).
        for ((country, scheme) in NationalIdGenerator.supported) {
            items += CatalogItem("National ID", "${scheme.label} (${country.code}) · ${country.displayName}") { seed ->
                NationalIdGenerator.generate(country, Rng(seed), valid = true)
            }
        }

        // 5. Tax / VAT — supported schemes.
        for ((country, scheme) in TaxIdGenerator.supported) {
            items += CatalogItem("Tax ID", "${scheme.label} (${country.code}) · ${country.displayName}") { seed ->
                TaxIdGenerator.generate(country, Rng(seed), valid = true)
            }
        }

        // 6. Persona — coherent bundle, every country.
        for (country in Country.entries.sortedBy { it.displayName }) {
            items += CatalogItem("Persona", "Persona (${country.code}) · ${country.displayName}") { seed ->
                PersonaGenerator.generate(country, seed).formatted()
            }
        }

        return items
    }
}
