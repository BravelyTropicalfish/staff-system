# staffing/post-requirement — delta spec

Scope: the post a project is short of — its fields, its skill requirements, its derived status
and the rules that protect an already-confirmed person.

## ADDED Requirements

### Requirement: Post fields are validated on save

The system SHALL store a post as project, optional title, role, required job level band,
headcount, start date, end date and travel requirement. A save SHALL be refused unless
headcount is a whole number of at least 1, the date range is not inverted, and the role is an
active catalog role. A refused save SHALL store nothing. The post carries its own dates; the
spec does not require them to sit inside the project's dates (open item D7 in the proposal).

**Traces:** proposal step 4; REQ-001 §3, §5; S1.

#### Scenario: A valid post is created open

- **GIVEN** project `Atlas` running from 2027-01-01 to 2027-12-31
- **WHEN** the lead saves a `DEV` post, minimum `Senior`, headcount 2, travel `limited`, from
  2027-02-01 to 2027-06-30
- **THEN** the post is stored with status `open` and remaining headcount 2

#### Scenario: A fractional headcount is refused

- **GIVEN** the post form
- **WHEN** the lead submits headcount 1.5
- **THEN** the save is refused with a reason naming the whole-number rule

### Requirement: Skill requirements are typed and banded

The system SHALL store zero or more skill requirements per post, each with a catalog skill, a
kind (`required` or `bonus`) and a minimum or target proficiency band. A `required` row SHALL
gate eligibility; a `bonus` row SHALL only contribute to the fit score. A post SHALL be
savable with no skill requirement at all.

**Traces:** proposal step 4; REQ-001 §3, §5; S1, S18.

#### Scenario: Required and bonus rows are distinguished

- **GIVEN** a post with 后端开发 as `required` at `Practitioner` and 测试 as `bonus` at
  `Practitioner`
- **WHEN** the post is saved and shown
- **THEN** the two rows carry their own kind and band, and the bonus row is labelled as
  ranking only

#### Scenario: A post without skills is valid

- **GIVEN** the post form with no skill row added
- **WHEN** the lead saves the post
- **THEN** the post is stored and a candidate search for it can run

### Requirement: Status is derived from the remaining headcount

The system SHALL derive status from the count of active assignments: `open` when no person is
confirmed, `partially filled` when at least one but fewer than `headcount` are confirmed, and
`filled` when the count equals `headcount`. Status SHALL NOT be editable by hand, and a
`filled` post SHALL be absent from the project's "still short" list.

**Traces:** proposal step 4 and the "New capabilities" row for `post-requirement`;
REQ-001 §3, §5; S1, S7, S16, S27.

#### Scenario: Filling the last slot

- **GIVEN** a headcount-2 post with one confirmed person
- **WHEN** the second person is confirmed
- **THEN** the status becomes `filled` and the post leaves the "still short" list

#### Scenario: An inactive confirmed person keeps the post filled

- **GIVEN** a `filled` post whose confirmed person the report marks inactive
- **WHEN** the gap view is opened
- **THEN** the post stays `filled` and the person is flagged as inactive there

### Requirement: Gate-bearing fields freeze while a person is confirmed

While a post has at least one active assignment, the system SHALL refuse to change its role,
required job level, `required` skill rows, start date, end date or travel requirement. The
refusal SHALL name the confirmed people and require their release first, and SHALL change
nothing — neither the post nor its pending recommendations nor its assignments.

**Traces:** proposal step 7 and key rule "post lifecycle"; REQ-001 §6
post lifecycle; S20.

#### Scenario: Dates cannot move under a confirmed person

- **GIVEN** a post with one confirmed person and one pending recommendation
- **WHEN** the lead changes the end date
- **THEN** the edit is refused
- **AND** the post, the pending recommendation, the assignment and its allocation are
  unchanged

#### Scenario: The lock spreads to every gate-bearing field

- **GIVEN** a post with one confirmed person
- **WHEN** the lead tries to change the role, the required job level, a `required` row, the
  start date or the travel requirement
- **THEN** each attempt is refused with the release-first reason

### Requirement: Headcount may rise but never fall below the confirmed count

While a post has confirmed people, the system SHALL accept a higher headcount, moving the post
back to `partially filled` when it was `filled`, and SHALL refuse a headcount lower than the
number of active assignments.

**Traces:** proposal step 7; REQ-001 §6; S26, S27.

#### Scenario: Raising the headcount reopens a filled post

- **GIVEN** a `filled` post with headcount 2 and two confirmed people
- **WHEN** the lead raises the headcount to 3
- **THEN** the post becomes `partially filled` with remaining headcount 1 and reappears in the
  "still short" list

#### Scenario: Lowering below the confirmed count is refused

- **GIVEN** a post with headcount 2 and two confirmed people
- **WHEN** the lead lowers the headcount to 1
- **THEN** the edit is refused and both assignments remain

### Requirement: Editable changes void pending recommendations

The title and the `bonus` skill rows SHALL stay editable while people are confirmed. Such an
edit SHALL void every pending recommendation of that post with reason `post changed`, SHALL
leave confirmed assignments and their allocations untouched, and SHALL list each voided
recommendation in the coordinator's notice list.

**Traces:** proposal step 7; REQ-001 §6; S25.

#### Scenario: A bonus target change voids only pending recommendations

- **GIVEN** a post with one pending recommendation and one confirmed person
- **WHEN** the lead raises a `bonus` target band
- **THEN** the pending recommendation becomes `voided` with reason `post changed`
- **AND** the confirmed assignment and its allocation are unchanged

### Requirement: Cancelling is refused while a person is confirmed

The system SHALL refuse to cancel a post that has an active assignment and SHALL name the
people to release first. Once no assignment remains, a cancellation SHALL be accepted and
SHALL void every pending recommendation with reason `post cancelled`.

**Traces:** proposal step 7; REQ-001 §6; S21.

#### Scenario: Cancel is refused, then allowed

- **GIVEN** a post with one confirmed person and one pending recommendation
- **WHEN** the lead cancels the post
- **THEN** the cancellation is refused
- **WHEN** the person is released and the lead cancels again
- **THEN** the post becomes `cancelled` and the pending recommendation is `voided` with
  reason `post cancelled`

### Requirement: Only the owning lead creates and edits a post

The system SHALL let a lead create, edit and cancel posts of projects they own and SHALL
refuse those actions on any other project. The coordinator SHALL read every post and every
gap but SHALL NOT create, edit or cancel one.

**Traces:** proposal step 4; REQ-001 §2, §3; S10, S23.

#### Scenario: A coordinator cannot edit a post

- **GIVEN** a post of project `Atlas`
- **WHEN** the coordinator opens it
- **THEN** the post is readable and no save or cancel action is offered
