# staffing/headcount-report — delta spec

Scope: the headcount report — two bar charts (by role, by grade) over the active
people pool, read once and rendered inside the existing people pool screen. The
report owns no state: it derives every number from rows REQ-001 already stores.

## ADDED Requirements

### Requirement: The report describes the active pool only

The system SHALL count only people whose `active` flag is true, and SHALL answer, from one
read-only request that writes nothing, the number of those people together with their
grouping by role and their grouping by job level. The total SHALL equal the sum of the
role counts and the sum of the grade counts, because both groupings describe the same
pool. An inactive person SHALL appear in neither grouping and in neither total.

**Traces:** proposal step 1 and step 2, key rule "One pool, two axes" and "Active only";
REQ-002 §3, §5; S1, S2.

#### Scenario: Both charts describe one pool

- **GIVEN** a pool of 113 active people spread over four roles and three job levels
- **WHEN** the report is read
- **THEN** the total is 113, the role counts sum to 113 and the grade counts sum to 113
- **AND** the two charts show that same total

#### Scenario: An inactive person is in neither chart

- **GIVEN** a pool whose total is 113
- **WHEN** one more person is imported with `active` false
- **THEN** the total stays 113 and no bar moves

### Requirement: Every category on the axis keeps its bar, ordered by the model

The system SHALL answer one bar for each category on the axis: every role that the catalog still
offers (`active`), plus every role that holds at least one active person, plus the four job-level
bands. A category whose count is zero SHALL keep its bar at height zero and SHALL NOT be dropped
for being empty. Role bars SHALL be ordered by the role's position in the catalog, grade bars by
the band order `Junior` < `Senior` < `Lead` < `Principal`; neither order is by size. A bar SHALL be
named by the stored role name, so a role deactivated in the catalog keeps its bar while its
active people remain counted; a deactivated role that holds nobody SHALL leave the axis, because
the catalog no longer offers it.

**Traces:** proposal step 4, key rule "Empty categories keep their place", "Order comes from the
model" and "A deactivated role still counts its people"; REQ-002 §3, §5, B-3, B-3b, B-4, B-5,
B-6, B-11; S5, S6, S7, S11.

#### Scenario: A category with nobody keeps its bar

- **GIVEN** no active person holds the role `QA` and none sits at `Principal`
- **WHEN** the report is read
- **THEN** the role grouping carries a `QA` bar and the grade grouping carries a `Principal` bar
- **AND** each of them is drawn at height zero and reads `0` with a share of `0.0`

#### Scenario: Role bars follow catalog order

- **GIVEN** active counts of PM 40, BA 25 and DEV 34, with `QA` and `UX` on the axis holding
  nobody
- **WHEN** the report is read
- **THEN** the role grouping is ordered PM, BA, DEV, QA, UX in catalog order
- **AND** the two zero bars are present and the order is not by size

#### Scenario: Grade bars follow band order

- **GIVEN** active counts of Senior 42, Junior 38 and Lead 28, with `Principal` holding nobody
- **WHEN** the report is read
- **THEN** the grade grouping is ordered Junior, Senior, Lead, Principal
- **AND** the largest count is not first and the zero band is last rather than missing

#### Scenario: A deactivated role keeps its active people counted

- **GIVEN** a role that the coordinator has deactivated while three active people still hold it
- **WHEN** the report is read
- **THEN** that role has a bar under its stored name and the total is unchanged
- **AND** a second deactivated role holding nobody is absent from the axis

### Requirement: Each bar carries its headcount and its share of the total

Each bar SHALL carry its raw headcount and its share of the total as a percentage with
one decimal, rounded half-up, so a count of 40 out of 113 reads `35.4`. A category that
holds the whole pool SHALL read `100.0`. The shares SHALL NOT be adjusted to add up to
exactly 100.0: a residue that leaves them at 99.9 or 100.1 SHALL be accepted, and the
counts SHALL stay exact.

**Traces:** proposal step 3, key rule "Shares are rounded once, and never corrected";
REQ-002 §5, B-8, B-9, B-10; S3, S4.

#### Scenario: A bar carries its count and its share

- **GIVEN** 40 active people hold the role `PM` in a pool of 113
- **WHEN** the report is read
- **THEN** that bar answers a headcount of 40 and a share of 35.4

#### Scenario: A share rounds to one decimal

- **GIVEN** pools of three people (one and two of them in one role) and of eight (one)
- **WHEN** the report is read for each
- **THEN** the shares read 33.3, 66.7 and 12.5 respectively

### Requirement: The two charts are a section of the people pool screen

The system SHALL render both charts, each with its total, as a section of the existing
people pool screen, above the person table. Each bar SHALL be drawn proportionally to its
count and carry its headcount and its share in text. No separate screen and no additional
route SHALL be introduced for the report. The person table SHALL keep listing every
imported person, inactive ones included, so the screen shows the pool's rows and the
active pool's shape together.

**Traces:** proposal step 5, key rule "Two numbers on one screen, two meanings";
REQ-002 §3, §6, B-14; S8, S12.

#### Scenario: Both charts sit on the pool screen

- **GIVEN** a signed-in coordinator
- **WHEN** they open `/people`
- **THEN** the two charts appear above the person table with their totals
- **AND** no further screen is reachable for the report

#### Scenario: The table and the charts differ on purpose

- **GIVEN** a pool holding one inactive person among the active ones
- **WHEN** the coordinator reads that screen
- **THEN** the table lists the inactive person
- **AND** the total and every bar count only the active ones

### Requirement: The report is the coordinator's alone

The system SHALL answer the report to a resource coordinator, and SHALL refuse a project
lead with `403` without answering any number. The pool screen SHALL render the chart
section only for the coordinator: a lead opening the screen SHALL see no chart section, no
placeholder and no refusal message, and the system SHALL NOT answer the report to them on
that screen.

**Traces:** proposal key rule "The report is the coordinator's, and a lead sees no trace of
it"; REQ-002 §2, §6, B-12, B-13; S9.

#### Scenario: A lead sees no report

- **GIVEN** a user holding only the lead role
- **WHEN** they open `/people` and, separately, request the report directly
- **THEN** the screen draws no chart section and requests no report
- **AND** the direct request answers `403`

### Requirement: An empty pool is stated, not drawn

The system SHALL answer a total of `0` and two groupings whose every count is `0` when no person
is active, and the screen SHALL print that the pool has no active person instead of drawing a
bar or a share — this empty state SHALL replace both charts, zero bars included, because a chart
whose tallest bar is zero has no scale. The report SHALL NOT divide by the total in that case.

**Traces:** proposal step 2 and step 5; REQ-002 §6, B-7; S10.

#### Scenario: An empty pool says so

- **GIVEN** a pool whose every person is inactive, or a pool with no person at all
- **WHEN** the coordinator reads the pool screen
- **THEN** both chart areas print that no active person is there, with a total of 0
- **AND** no bar, no share and no division by zero appears
