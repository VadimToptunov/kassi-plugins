# Kassi Plugins

[![Build](https://github.com/VadimToptunov/kassi-plugins/actions/workflows/build.yml/badge.svg)](https://github.com/VadimToptunov/kassi-plugins/actions/workflows/build.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Open-source JetBrains / Android Studio plugins for QA, fintech and KYC engineering, built on **one
shared, fully-tested data engine**. MIT-licensed.

Generic faker tools produce *plausible-looking* data. This family produces **spec-valid** data that
actually passes a real validator (IBAN mod-97, Luhn, VAT, BSN, ABN, TFN, ICAO check digits …) — and
tools that validate such data. Everything is 100% offline and algorithmic: zero network, zero real PII.

## Plugins

| Plugin | What it does | Marketplace |
|--------|--------------|-------------|
| **Checksum & Regex Playground** | Live-validate any pasted identifier against every applicable checksum, plus a regex tester. | [![Version](https://img.shields.io/jetbrains/plugin/v/33227.svg?label=marketplace)](https://plugins.jetbrains.com/plugin/33227-checksum--regex-playground) [![Downloads](https://img.shields.io/jetbrains/plugin/d/33227.svg)](https://plugins.jetbrains.com/plugin/33227-checksum--regex-playground) |
| **UUID, ULID & NanoID Toolkit** | Generate and validate UUID/ULID/NanoID, with hover info (UUID version, timestamp from a ULID). | [![Version](https://img.shields.io/jetbrains/plugin/v/33488.svg?label=marketplace)](https://plugins.jetbrains.com/plugin/33488) [![Downloads](https://img.shields.io/jetbrains/plugin/d/33488.svg)](https://plugins.jetbrains.com/plugin/33488) |
| **MRZ & Barcode Inspector** | Parse & validate passport/ID MRZ (TD1/TD2/TD3) and AAMVA PDF417 payloads with per-field check digits. | ✅ listed |
| **Secret & Card Leak Guard** | Inspection that flags real PAN/IBAN/SSN leaking into code and fixtures, with a quick-fix to synthetic test data. | _coming soon_ |
| **Kassi HTTP Data Injector** | Insert valid synthetic test data (PAN/IBAN/BIC/persona) into `.http` request files. | _coming soon_ |

The related **[Kassi Test Data](https://plugins.jetbrains.com/plugin/33149-kassi-test-data)** generator
is published separately.

## Structure

```
kassi-plugins/
├── engine/                     # :engine — pure Kotlin, zero IntelliJ deps. Checksums, registries,
│                               #   generators, inspectors. All validity tests live here.
└── plugins/<name>/             # one IntelliJ plugin per module, each depending on :engine
```

The engine is the correctness moat: every generator/checker has a unit test asserting valid output
passes the real algorithm and the invalid variant fails it.

## Build

```bash
./gradlew :engine:test                          # run the validity test suite
./gradlew :plugins:checksum-playground:runIde   # try a plugin in a sandbox IDE
./gradlew :plugins:checksum-playground:buildPlugin
```

Requires JDK 17. Built with the IntelliJ Platform Gradle Plugin. Compatible with IntelliJ IDEA,
Android Studio and other IntelliJ-based IDEs (since-build 232).

## License

MIT — see [LICENSE](LICENSE).
