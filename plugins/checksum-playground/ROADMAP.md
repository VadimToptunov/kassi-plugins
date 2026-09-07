# Checksum & Regex Playground — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Every new checksum must have a validity test anchored to an
**external published reference** value, not one recomputed with our own algorithm.

## 1.11.0 — More checksum types
Add SIREN / SIRET (FR), Codice Fiscale (IT), DNI / NIE (ES), NHS number (UK), GTIN-8 / GTIN-14,
ICCID — each live-validated like the existing set.

## 1.12.0 — Batch mode
Paste a list / column of values → a pass/fail table against a chosen check. Ideal for QA-ing a
test-data fixture file at once.

## 1.13.0 — Regex tab upgrades
Replace-with-preview, a named-capture-group table, a library of ready patterns (IBAN / card /
email), and inline flag toggles (multiline / ignore-case / dotall).
