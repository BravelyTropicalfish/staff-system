# staffing/people-import Specification

## Purpose
TBD - created by archiving change add-staffing-mvp. Update Purpose after archive.

## Requirements

### Requirement: One workbook, one sheet

The system SHALL accept exactly one `.xlsx` workbook per upload, holding one `people` sheet with
the columns `employee_no`, `name`, `email`, `role`, `job_level`, `travel_willingness`, `active`
and `skills`. A row SHALL be one person plus that person's skills: the `skills` cell holds
`skill:band` pairs separated by semicolons (`后端开发:Practitioner;测试:Expert`) and MAY be blank
for a person who carries no skill change. A file whose sheet holds no data row SHALL be rejected.

**Traces:** proposal step 2; REQ-001 §5.1; S32.

#### Scenario: One row carries several skills

- **GIVEN** a `people` row whose `skills` cell reads `Java:Novice; 测试:Practitioner`
- **WHEN** the coordinator uploads it
- **THEN** the batch is `applied`, the person carries both bands and nothing else changes

#### Scenario: A workbook without the people sheet is refused

- **GIVEN** a workbook holding only some other sheet
- **WHEN** the coordinator uploads it
- **THEN** the upload is refused and no batch is recorded at all

#### Scenario: An empty workbook is refused

- **GIVEN** a workbook whose `people` sheet carries headers only
- **WHEN** the coordinator uploads it
- **THEN** the upload is refused with a reason naming the empty file, and no batch is recorded

### Requirement: An import is atomic

The system SHALL validate every row of the sheet before writing anything. If at least one row
fails validation, the system SHALL write nothing — no person created, no band changed, no flag
flipped — SHALL record the batch as `rejected`, and SHALL list every failing row with sheet, row
number, key value and reason. A batch whose rows all validate SHALL be recorded as `applied`.

**Traces:** proposal step 2 and key rule "An import is all-or-nothing"; REQ-001 §5.1; S12,
S14, S29, S31.

#### Scenario: One bad row blocks the whole file

- **GIVEN** a workbook of five `people` rows where one row carries `job_level` = `Mid` and
  another row's `skills` cell names skill `Kotlin`
- **WHEN** the coordinator uploads it
- **THEN** the batch is `rejected`
- **AND** all five people keep exactly the data they had before the upload
- **AND** the error list holds one entry per bad row, each with sheet, row number, key and
  reason

#### Scenario: The corrected file is a new batch

- **GIVEN** the rejected batch above
- **WHEN** the coordinator corrects both rows and uploads the file again
- **THEN** a new batch is `applied` and every row is written
- **AND** the earlier batch keeps its own status and error list

#### Scenario: A rejected batch stays readable

- **GIVEN** a rejected batch exists
- **WHEN** the coordinator opens the batch history
- **THEN** the batch shows file name, uploader, timestamp, row counts, status and its error
  list, all read-only

### Requirement: Row validation rules

Before anything is written, the system SHALL apply these rules to every row. `employee_no`,
`name`, `role`, `job_level`, `travel_willingness` and `active` SHALL be non-blank, while `email`
and `skills` MAY be blank. `employee_no` SHALL be unique within the file. `role` SHALL be exactly
one active catalog role — a cell naming two roles, or a delimited list, is a row error and is
never split into two roles. Each `skill:band` item in the `skills` cell SHALL name an active
catalog skill and one of the five proficiency bands; an item without a colon, a skill the catalog
does not hold, a band outside the five, or the same skill twice in one cell SHALL be a row error
and SHALL reject the whole row. `job_level` SHALL be one of the four bands, `travel_willingness`
one of `none` / `limited` / `full` and `active` one of `active` / `inactive`. Cells SHALL be
trimmed, and ASCII values SHALL match case-insensitively, the stored value being the catalog
spelling.

**Traces:** proposal step 2; REQ-001 §5.1; S12, S14, S17, S29, S30, S31.

#### Scenario: Lower case is normalised, not rejected

- **GIVEN** a `people` row with `job_level` = `senior`, `active` = `Active` and `skills` =
  `测试:practitioner`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `applied` and the profile stores `Senior`, `active` and
  `Practitioner`

#### Scenario: A role outside the catalog blocks the file

- **GIVEN** a `people` row with `role` = `Product`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and the reason lists the accepted roles

#### Scenario: A multi-role cell is a row error, never two roles

- **GIVEN** a `people` row whose `role` cell reads `DEV, QA`, and another whose cell reads
  `DEV / QA`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` with a row error on the `role` column for each row
- **AND** no person is created or stored holding two roles, because the cell is never split

#### Scenario: The same skill twice in one cell blocks the file

- **GIVEN** a `people` row whose `skills` cell reads `测试:Novice;测试:Expert`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and the reason names the repeated skill on that row

#### Scenario: A skill outside the catalog blocks the file

- **GIVEN** a `people` row whose `skills` cell names skill `Kotlin`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and the reason says the skill is not in the active catalog

#### Scenario: A duplicated employee number blocks the file

- **GIVEN** two `people` rows carrying the same employee number
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and the reason names the duplicated number

### Requirement: An applied batch writes person and skill data

When every row validates, the system SHALL create a person for each unknown employee number,
overwrite the stored values of the fields carried in the file, and insert or overwrite a
proficiency band for each `skill:band` item in the `skills` cell. It SHALL NOT delete a person, a
skill band or any other record, and SHALL store the batch as `applied` with its row counts.

**Traces:** proposal step 2; REQ-001 §5.1; S13, S15, S16.

#### Scenario: New and existing people in one file

- **GIVEN** the system holds `E100` and the file holds `E100` with a new job level plus
  `E200`, unknown to the system
- **WHEN** the coordinator uploads the file
- **THEN** `E200` is created with the role, job level, travel willingness and active flag of
  the file, and `E100` carries the new job level
- **AND** the batch is `applied`

#### Scenario: An inactive person is reactivated by the report

- **GIVEN** `E100` is stored inactive and the file carries `active` = `active` for `E100`
- **WHEN** the coordinator uploads the file
- **THEN** the profile shows `E100` as active

### Requirement: Import never touches staffing state

An import SHALL NOT create, change or delete an allocation, a post, a recommendation or an
assignment. The next candidate search SHALL simply use the imported data.

**Traces:** proposal step 2; REQ-001 §5.1 side effects, §9; S11, S16.

#### Scenario: Allocations survive an import

- **GIVEN** `E100` has an allocation on `Atlas` and the file reports a new travel willingness
- **WHEN** the coordinator uploads the file
- **THEN** the allocation is unchanged and the new willingness is shown on the profile
