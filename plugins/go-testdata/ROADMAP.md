# Go Test Data — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Generated values come from the shared, unit-tested Kassi engine
(spec-valid, checksummed), so every emitted row carries its validity test — the real checksum passes
for the valid rows and fails for the invalid ones.

## 1.1.0 — More data kinds
Beyond IBAN and Card, emit table-driven cases for **BIC/SWIFT**, **national ID** and **VAT / Tax ID**
(reusing the Kassi generators), each with its spec-valid values and a deliberately-invalid variant.

## 1.2.0 — Fuzz seed corpus
Emit a Go 1.18+ **`FuzzXxx`** test with an `f.Add(...)` seed corpus of the valid and invalid values, so
the validator is both table-tested and fuzzed from known-good / known-bad seeds — Go-native, no
dependency.

## 1.3.0 — Coherent persona struct
Emit a **coherent persona** as a Go struct literal — name, date of birth, IBAN, BIC and tax ID all
consistent with one person — for integration tests that need a whole valid subject, not a single field.
