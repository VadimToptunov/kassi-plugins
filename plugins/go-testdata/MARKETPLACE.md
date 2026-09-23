# Go Test Data

**Tagline:** Generate idiomatic Go table-driven tests whose rows are spec-valid — and their invalid variants — offline.

## Overview

Writing a `_test.go` for a validator means hand-picking inputs: a few values that should pass and a
few that should fail. A generic faker gives you neither — its "IBAN" won't pass mod-97 and its "card"
won't pass Luhn, so your test never actually exercises the validator. **Go Test Data** generates an
idiomatic Go **table-driven test** whose rows come straight from the checksummed Kassi engine: every
"valid" row really passes the real checksum, and every "invalid" row really fails it.

- **IBAN** — ISO 7064 **mod-97**-valid values across several countries, each paired with a
  bad-checksum variant.
- **Card (PAN)** — **Luhn**-valid numbers per card network, each paired with a fails-Luhn variant.

Pick a kind, click **Generate**, and **Copy** the ready-to-paste `[]struct{ … }` table (with `name`,
`input`, and `valid` columns) plus its `t.Run` sub-test loop into your package. Fully offline, deterministic, zero telemetry —
the same shared, unit-tested engine behind the Kassi Test Data generator.

## Where it runs

Any JetBrains IDE — open **View ▸ Tool Windows ▸ Go Test Data**. Platform-only, so it installs in
**GoLand** and every other IntelliJ-based IDE (the generated code is plain Go text — no Go plugin
required to produce it).

## Category / tags

- Category: **Code tools** (test data / code generation).
- Tags: `go`, `golang`, `test-data`, `table-driven-test`, `testing`, `iban`, `card`, `luhn`, `mod97`,
  `fixtures`, `offline`.

## More from Kassi

Part of the Kassi test-data / QA line — see also **Kassi Test Data** (plugin 33149), **Checksum &
Regex Playground** (33227), **pytest Fixtures**, **SQL Synthetic Seeder**, and the **Secret & Card
Leak Guard** inspection.
