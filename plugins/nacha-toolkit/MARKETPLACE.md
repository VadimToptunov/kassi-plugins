# ACH / NACHA Toolkit

**Tagline:** Validate and generate US ACH (NACHA) files in the IDE — offline, no uploads.

## Overview

Payments engineers hand-craft and debug fixed-width **NACHA ACH** files — 94-character records, batch
and file control totals, entry hash, ABA routing check digits — and today they paste them into random
websites or eyeball them by hand. That's exactly the data that shouldn't leave your machine (in Nov 2025
two popular online code-formatting sites were found to have leaked 80,000+ pasted blobs, including
banking credentials). **ACH / NACHA Toolkit** does it **offline**: no browser, no upload, no telemetry.

- **Validate** — paste or open an ACH file and get a precise problem list: wrong record length, bad
  **ABA routing check digit** on an entry, and mismatched **entry hash**, **total debit / credit**,
  **entry count** or **block count** in the batch/file control.
- **Generate** — produce a spec-valid single-batch **PPD** sample file (correct control totals and entry
  hash) and a deliberately **invalid** variant for negative tests, then copy it.
- **Routing numbers** — the ABA routing-transit check digit (weights 3-7-1, mod 10) is the same check
  real Federal Reserve routing numbers pass.

## Where it runs

Any JetBrains IDE — open **View ▸ Tool Windows ▸ ACH / NACHA**. Platform-only, so it installs in every
IntelliJ-based IDE. Fully offline and deterministic — the same validated engine behind the Kassi fintech
tools.

## Category / tags

- Category: **Code tools** (test data / validation).
- Tags: `ach`, `nacha`, `payments`, `fintech`, `banking`, `routing-number`, `aba`, `test-data`,
  `validation`, `offline`.

## More from Kassi

Part of the Kassi fintech / QA line — see also **Kassi Test Data** (plugin 33149), **ISO 8583** and
**SWIFT MT Validator** (financial message validators), and **Checksum & Regex Playground** (33227).
