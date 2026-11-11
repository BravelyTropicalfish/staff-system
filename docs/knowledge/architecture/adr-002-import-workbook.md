# ADR-002 — Reading the people workbook

- **Status:** accepted, 2026-11-11
- **Decision owner:** the change's design (`openspec/changes/add-staffing-mvp/design.md` §7 for the
  import flow, the open items D4 and D6 recorded in the same file)
- **Change:** `add-staffing-mvp` (`staffing/people-import`)

## Context

The workbook upload is the only channel for person master data, and one upload must be one unit of
work: validate every row, then write all of it or none of it. Two things were unknown when the
change started — the columns of the real HR export, and what a coordinator should see when a file
cannot be read at all versus when its rows are wrong. The repository requires business rules kept
out of both the UI and the database code.

## Decision

- **`org.apache.poi:poi-ooxml` 5.3.0** reads `.xlsx`. It is the only new dependency; POI pulls
  `log4j-api` and never `log4j-core`, so the Log4Shell class of issue does not apply.
- **Reading, translating and judging are three classes.** `WorkbookParser` owns the shape of the
  file (both sheets, header row, at least one data row, blank rows skipped, spreadsheet row numbers
  kept). `ColumnMap` maps header labels to columns after stripping case, spaces and underscores,
  which is the one place a real export's spelling is absorbed. `ImportValidator` applies every row
  rule and returns rows in catalog spelling plus a list of `RowError`.
- **A file that cannot be read is not a batch.** Wrong shape answers 422 `unreadable workbook` and
  writes nothing. A readable file with bad rows is stored as a `REJECTED` batch with its full error
  list and is answered with 201, because the history entry now exists.
- **One transaction per upload.** A batch row is inserted once with uploader, row counts and status,
  and the person and band writes commit with it. Applying never deletes, and never touches an
  allocation, post, recommendation or assignment.
- **A cell is never split.** A role cell naming two roles is a row error, so one person can never
  end up holding two roles through an import.

## Consequences

- The coordinator sees two different answers — "this file is not a workbook" and "this file has
  three bad rows" — which keeps the "no batch at all" rule of the contract observable.
- A real export with different header spelling changes `ColumnMap` only; the rules and tests stay
  as they are.
- Reading the whole workbook into memory is acceptable for a person master file of MVP size; the
  parser refuses nothing for size, so a very large file would need streaming later.

## Alternatives considered

- **CSV** — a coordinator will not keep a CSV in step with an Excel export; the contract names
  `.xlsx`, so a workbook it is.
- **Validating while writing row by row** — simpler, but it breaks the all-or-nothing rule the
  contract states and leaves half a file behind on the first bad row.
- **Apache Commons CSV or a hand-rolled zip/XML reader** — loses cell formatting and number
  handling POI already does (an employee number written as a number must read as its digits).
- **Storing the reason in the batch row only** — the contract asks for sheet, row number and key
  value per failing row, so `import_error` carries them.

## Verification

- `WorkbookParserTest` (shape) and `ImportValidatorTest` (every row rule) run without a database.
- `ImportApiTest` proves applied and rejected batches, the 422 path leaving no batch, reactivation,
  a skills-only refresh, and an allocation surviving an import.
