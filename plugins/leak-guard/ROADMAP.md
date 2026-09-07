# Secret & Card Leak Guard — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Every detector keeps the "real vs synthetic test data"
discrimination, and each checksum-based check has a validity test anchored to an **external
published reference**.

## 1.1.0 — More detectors
Real-looking secrets/tokens (AWS access keys, JWTs, PEM private-key blocks), email + phone PII, and
more national IDs (UK NINo, DE, …) — each distinguishing real values from safe test data.

## 1.2.0 — CI mode
Headless SARIF export + baseline (as in Flaky Test Linter) so the inspection runs as a pre-commit /
PR gate outside the IDE.

## 1.3.0 — Configurable allowlist / severity
Per-rule enable + severity, a project-level ignore for known-safe fixtures, and a quick-fix to add
an inline suppression comment.
