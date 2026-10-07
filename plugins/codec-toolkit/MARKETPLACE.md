# Crypto Codec Toolkit

**Tagline:** Base58 / Base58Check / Base32 / hex conversions in the IDE — the crypto encodings generic string tools skip, fully offline.

## Overview

General "string manipulation" plugins already do Base64, URL and plain hex. **Crypto Codec Toolkit**
covers the encodings they skip, for people who work with crypto and wire formats: **Base58** and
**Base58Check** (Bitcoin addresses, WIF keys, extended keys), **Base32** (RFC 4648) and **hex** — all
**offline**, so an address, a payload or a key never has to be pasted into a web codec.

- **Any-to-any** — pick a **From** and a **To** representation (Text / Hex / Base58 / Base58Check /
  Base32); the input is decoded to raw bytes and re-encoded. e.g. **Hex → Base58Check** to build an
  address, or **Base58Check → Hex** to see a decoded payload.
- **Base58Check** appends and verifies the 4-byte double-SHA-256 checksum, so a bad address is rejected
  instead of silently decoded.
- Deterministic and offline — no telemetry, no network. The Base58Check and Base32 are anchored in tests
  to published vectors (the Bitcoin wiki worked example and RFC 4648 §10).

## Where it runs

Any JetBrains IDE — open **View ▸ Tool Windows ▸ Codec**. Platform-only, so it installs in every
IntelliJ-based IDE.

## Category / tags

- Category: **Code tools**.
- Tags: `base58`, `base58check`, `base32`, `hex`, `bitcoin`, `crypto`, `encode`, `decode`, `rfc4648`,
  `codec`, `offline`.

## More from Kassi

Part of the Kassi fintech / QA line — see also **Kassi Test Data** (plugin 33149), **Checksum & Regex
Playground** (33227), and the **ISO 8583** / **SWIFT MT** validators.
