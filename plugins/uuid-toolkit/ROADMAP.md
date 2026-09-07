# UUID, ULID & NanoID Toolkit — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Decode/generate tests are anchored to **external published
reference** vectors (RFC 9562 examples, known ULID/Snowflake samples).

## 1.7.0 — UUID v1 / v6 + Snowflake decode
Generate UUID v1 and v6; decode Snowflake IDs (Twitter / Discord epochs) to a timestamp, plus the
node / clock-seq fields of v1/v6.

## 1.8.0 — Bulk generate + formats
Generate N identifiers at once, with format options (uppercase / braces / `urn:uuid:`), and
copy-all / CSV / array output.

## 1.9.0 — Inline inspection
An editor inspection that decodes a UUID / ULID under the caret (version, variant, embedded
timestamp in a chosen time zone) without leaving the file.
