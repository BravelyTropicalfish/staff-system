# staffing/people-import — delta spec

Scope: the workbook upload that is the only channel for person master data. One upload is one
unit of work.

## ADDED Requirements

### Requirement: One workbook, two sheets

The system SHALL accept exactly one `.xlsx` workbook per upload, with a `people` sheet
carrying `employee_no`, `name`, `email`, `role`, `job_level`, `travel_willingness`, `active`
and a `skills` sheet carrying `employee_no`, `skill`, `proficiency`. Both sheets SHALL be
present. The `people` sheet MAY be empty so that a file refreshes skills only; a file with no
data row in either sheet SHALL be rejected.

**Traces:** proposal step 2; REQ-001 §5.1; S32.

#### Scenario: Skills-only refresh is accepted

- **GIVEN** a workbook whose `people` sheet is empty and whose `skills` rows name existing
  employees with catalog skills
- **WHEN** the coordinator uploads it
- **THEN** the batch is `applied`, the listed bands change and no person is created

#### Scenario: A workbook with one sheet is refused

- **GIVEN** a workbook holding only a `people` sheet
- **WHEN** the coordinator uploads it
- **THEN** the upload is refused and no batch is recorded as applied

#### Scenario: An empty workbook is refused

- **GIVEN** a workbook whose two sheets carry headers only
- **WHEN** the coordinator uploads it
- **THEN** the upload is refused with a reason naming the empty file

### Requirement: An import is atomic

The system SHALL validate every row of both sheets before writing anything. If at least one
row fails validation, the system SHALL write nothing — no person created, no band changed, no
flag flipped — SHALL record the batch as `rejected`, and SHALL list every failing row with
sheet, row number, key value and reason. A batch whose rows all validate SHALL be recorded as
`applied`.

**Traces:** proposal step 2 and key rule "an import is all-or-nothing"; REQ-001 §5.1; S12,
S14, S29, S31.

#### Scenario: One bad row blocks the whole file

- **GIVEN** a workbook of five `people` rows where one row carries `job_level` = `Mid` and
  one `skills` row names skill `Kotlin`
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
`name`, `role`, `job_level`, `travel_willingness` and `active` SHALL be non-blank in a
`people` row, while `email` MAY be blank. `employee_no` SHALL be unique within the file.
`role` SHALL be exactly one active catalog role — a cell naming two roles, or a delimited list,
is a row error and is never split into two roles; `skill` SHALL be a catalog skill, `job_level` SHALL
be one of the four bands, `proficiency` one of the five bands, `travel_willingness` one of
`none` / `limited` / `full` and `active` one of `active` / `inactive`. A second `skills` row
for the same employee number and skill SHALL be an error, and so SHALL a `skills` row whose
employee number appears neither in the system nor in the file's `people` sheet. Cells SHALL
be trimmed, and ASCII values SHALL match case-insensitively, the stored value being the
catalog spelling.

**Traces:** proposal step 2; REQ-001 §5.1; S12, S14, S17, S29, S30, S31.

#### Scenario: Lower case is normalised, not rejected

- **GIVEN** a `people` row with `job_level` = `senior` and `active` = `Active`, and a
  `skills` row with `proficiency` = `practitioner`
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

#### Scenario: A duplicated skill row blocks the file

- **GIVEN** two `skills` rows for the same employee number and skill `测试`
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and both row numbers appear in the error list

#### Scenario: An orphan skill row blocks the file

- **GIVEN** a `skills` row for employee number `E999`, absent from both the system and the
  file's `people` sheet
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` with the reason `person not found` for that row

#### Scenario: A duplicated employee number blocks the file

- **GIVEN** two `people` rows carrying the same employee number
- **WHEN** the coordinator uploads the file
- **THEN** the batch is `rejected` and the reason names the duplicated number

### Requirement: An applied batch writes person and skill data

When every row validates, the system SHALL create a person for each unknown employee number,
overwrite the stored values of the fields carried in the file, and insert or overwrite a
proficiency band for each `skills` row. It SHALL NOT delete a person, a skill band or any
other record, and SHALL store the batch as `applied` with its row counts.

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
