# pytest Fixtures — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Generated values come from the shared, unit-tested Kassi engine
(spec-valid, checksummed), so every emitted case carries its validity test — the real checksum passes
for the valid rows and fails for the invalid ones.

## 1.1.0 — More data kinds
Beyond IBAN and Card, emit parametrized cases for **BIC/SWIFT**, **national ID** and **VAT / Tax ID**
(reusing the Kassi generators), each with its spec-valid values and a deliberately-invalid variant.

## 1.2.0 — `@pytest.fixture` style (not just parametrize)
In addition to the `@pytest.mark.parametrize` table, emit a reusable **`@pytest.fixture`** that returns
a valid value (and a companion `*_invalid` fixture) — the fixtures the plugin's name promises, droppable
into a `conftest.py`.

## 1.3.0 — Coherent persona fixture
Emit a **coherent persona** as a Python dataclass (or dict) fixture — name, date of birth, IBAN, BIC and
tax ID all consistent with one person — for integration tests that need a whole valid subject, not a
single field.
