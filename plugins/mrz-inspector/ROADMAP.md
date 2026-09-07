# MRZ & Barcode Inspector — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Check-digit / parsing tests are anchored to **external published
reference** documents (ICAO Doc 9303, AAMVA), not self-recomputed values.

## 1.5.0 — PDF417 / AAMVA driver-license parser
Parse the AAMVA data string of a US/CA driver-license PDF417 barcode into fields — delivering the
"Barcode" half of the plugin's name (currently MRZ-only).

## 1.6.0 — More MRZ formats + transliteration
Additional issuing-country MRZ variants and ICAO 9303 name transliteration, with transliteration
correctness checks.

## 1.7.0 — Fix / round-trip action
Given a value with a wrong check digit, show the correct one; and build a valid MRZ from entered
fields (round-trip with the generator discipline).
