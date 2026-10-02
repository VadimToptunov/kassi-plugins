# ACH / NACHA Toolkit — roadmap

Planned features, shipped one per minor via the Kassi rotating auto-publisher
(`kassi-devtools/.github/publish-queue.tsv`). Each ships only when merged on master and
verifier-green (IC-232 + IC-252). Every format/checksum addition must have a validity test anchored
to an **external published reference** (the Nacha Operating Rules record layouts, real Federal Reserve
routing numbers, or a published sample ACH file), never a value recomputed with our own algorithm.

## 1.1.0 — Addenda records (type 7)
Generate and validate PPD/CCD **addenda records** (addenda type 05): the payment-related 80-char
free-text field, the addenda sequence number, and the entry-detail sequence number — with the entry's
addenda-record indicator and the batch/file **entry + addenda count** updated accordingly. Anchor: the
Nacha addenda record layout + a published sample file.

## 1.2.0 — More SEC codes and debit files
Support **CCD** (corporate), **WEB** and **TEL** standard-entry-class codes and debit-only files
(service class 225) with the correct transaction codes (27/37 debits, 22/32 credits) and company-entry
descriptions. Anchor: the Nacha SEC-code rules and transaction-code table.

## 1.3.0 — Parse to a labelled field table
Turn a loaded ACH file into a **field-by-field table** in the tool window: each record (File Header,
Batch Header, Entry Detail, Batch/File Control) expanded into its named fields with values, so a file
can be read, not just pass/fail-validated. Anchor: the Nacha fixed-width field positions.
