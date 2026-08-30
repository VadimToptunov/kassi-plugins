# Kassi HTTP Data Injector

**Tagline:** Insert spec-valid banking / KYC test data straight into your `.http` requests — offline, in your IDE.

## Overview

Writing or debugging an HTTP request in a JetBrains `.http` scratch file and need a **valid** IBAN,
card number, BIC or a whole persona to paste into the body? This plugin generates spec-valid synthetic
test data at the caret — via an intention and an editor-popup action — without leaving the editor and
without a single byte going to a server.

- **IBAN** — every European country, ISO 7064 **mod-97** valid (plus an invalid variant for negative tests).
- **Card (PAN)** — **Luhn**-valid, from payment-gateway **test** BIN ranges only, per network (Visa / Mastercard / Amex).
- **BIC/SWIFT** — structurally valid (ISO 9362), per country.
- **Persona** — a coherent identity (name, DOB, address, IBAN, BIC) as a ready-to-paste JSON object.

All values are synthetic and algorithmically valid — the same shared, unit-tested generator engine used
across the Kassi test-data tools. 100% offline, zero telemetry, no real PII.

## Where it runs

`.http` request files in any JetBrains IDE with the HTTP Client (IntelliJ IDEA Ultimate, and others
that bundle it). The insertion is an editor intention + popup action at the caret.

## Category / tags

- Category: **Developer Tools** (test data).
- Tags: `test data`, `http client`, `iban`, `luhn`, `bic`, `swift`, `persona`, `fintech`, `qa`, `offline`.

## More from Kassi

Part of the Kassi test-data line — see also the Kassi Test Data generator, Checksum Playground, UUID
Toolkit, MRZ Inspector, and the paid ISO 8583 / SWIFT MT validators.
