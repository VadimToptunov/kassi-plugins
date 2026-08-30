# Secret & Card Leak Guard

**Tagline:** Catch real card numbers, IBANs and SSNs before they hit a commit — inline, offline.

## Overview

Test files, fixtures and scratch code are where real PII quietly leaks into a repo. Leak Guard is a
whole-file inspection that flags values which look **real** (not synthetic test data) so you can scrub
them before they're committed — with a one-click quick-fix that replaces the value with a valid
synthetic equivalent.

- **Card numbers** — **Luhn**-valid, but the BIN is **not** one of the well-known payment-gateway test
  ranges → looks like a real card.
- **IBAN** — passes the ISO 7064 **mod-97** check, but isn't one of Kassi's own reserved test IBANs.
- **US SSN** — structurally issuable (area / group / serial outside the SSA's never-issued ranges).

Each finding offers **Replace with synthetic** — swapping in a spec-valid test value from the shared,
unit-tested Kassi engine, so your tests keep passing while the real datum is gone. Fully offline,
deterministic, zero telemetry.

## Where it runs

Any JetBrains IDE — it's a `localInspection` that runs on-the-fly and in batch (Analyze ▸ Inspect Code).

## Category / tags

- Category: **Inspection** (security / linters).
- Tags: `security`, `pii`, `secrets`, `leak`, `card`, `iban`, `ssn`, `luhn`, `inspection`, `offline`.

## More from Kassi

Part of the Kassi test-data / QA line — see also Kassi HTTP Data Injector, the Kassi Test Data
generator, Checksum Playground, and the paid ISO 8583 / SWIFT MT validators.
