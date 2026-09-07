# Kassi HTTP Data Injector — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Inserted values come from the shared, unit-tested Kassi generators
(spec-valid, checksummed), so every data type carries its validity test.

## 1.1.0 — More data types
Insert VAT numbers, national IDs, phone numbers and dates (reusing the Kassi generators), with
per-country pickers.

## 1.2.0 — Invalid-variant toggle + regenerate
A toggle to insert a deliberately invalid variant of any type (for negative tests), and a
"regenerate" action on an already-inserted value.

## 1.3.0 — Request templates
Insert a full request body template (e.g. a payment JSON) pre-filled with a coherent persona, and
support JetBrains HTTP Client environment variables.
