# staffing/allocation Specification

## Purpose
TBD - created by archiving change add-staffing-mvp. Update Purpose after archive.

## Requirements

### Requirement: A project is opened by its lead

The system SHALL store a project with name, lead, start date and end date, where the lead is one
identified lead account. A lead SHALL be able to open a project of their own and SHALL NOT open one
whose lead is somebody else. The coordinator SHALL NOT open a project: the coordinator reads every
project and writes none of them. A project whose end date precedes its start date SHALL be refused.

**Traces:** proposal step 3; REQ-001 §3, §5; S1, S39.

#### Scenario: A lead opens a project of their own

- **GIVEN** a user holding only the lead role
- **WHEN** they save a project naming themselves as the lead
- **THEN** the project is stored `open` with that lead
- **AND** a project naming another lead is refused and nothing is stored
- **AND** the coordinator saving any project is refused and nothing is stored

#### Scenario: Inverted dates are refused

- **GIVEN** the project form
- **WHEN** the coordinator submits end date 2027-03-31 with start date 2027-06-01
- **THEN** the save is refused and nothing is stored

### Requirement: Ending a project is terminal and releases its whole team

The system SHALL let the lead of a project end it exactly once. Ending SHALL be one atomic
change: the project becomes `closed`, each of its posts becomes `cancelled`, every pending
recommendation on those posts is voided with reason `post cancelled`, and every allocation of
the project is deleted. A `closed` project SHALL NOT be reopened, edited or given a new post.
The coordinator SHALL NOT end a project.

**Traces:** proposal step 9 and key rule "Ending a project"; REQ-001 §5, §6, §7 S39, S40; S39,
S40.

#### Scenario: The end releases everyone and cancels the posts

- **GIVEN** an `Atlas` project with two confirmed people, one `open` post and one pending
  recommendation
- **WHEN** the project's lead ends the project
- **THEN** the project is `closed`, both of its posts are `cancelled`, the pending
  recommendation is `voided` with reason `post cancelled`
- **AND** both people hold no allocation and pass G6 for a post in another project

#### Scenario: Only the project's lead may end it

- **GIVEN** a project whose lead is 陈立
- **WHEN** the coordinator sends the end request
- **THEN** the request is refused and the project stays `running`

#### Scenario: A closed project cannot be reopened

- **GIVEN** a project already `closed`
- **WHEN** its lead tries to reopen it or add a post to it
- **THEN** both attempts are refused and nothing changes

### Requirement: An allocation records a person on a project for an inclusive date range

The system SHALL store an allocation as person × project × inclusive start and end date. It
SHALL create one on every confirmed assignment, covering the post's dates, and SHALL let the
coordinator record one by hand for a person staffed outside the app. The coordinator SHALL be
able to change or remove a hand-entered allocation at any time; removing one deletes the record
without touching the project or its posts. An allocation written by a confirmation SHALL NOT be
changed or removed by hand: it goes away only when the person is released or the project ends,
which also remove the assignment it backs.

**Traces:** proposal step 3 and step 7, key rule "An allocation occupies its dates";
REQ-001 §5, §6; S4, S24, S39.

#### Scenario: Confirmation writes one allocation for the post's dates

- **GIVEN** a `proposed` recommendation on an `open` post of `Atlas` running 2027-02-01 to
  2027-06-30
- **WHEN** the lead confirms it
- **THEN** the person holds one allocation of `Atlas` from 2027-02-01 to 2027-06-30, with the
  source `confirmation`

#### Scenario: A hand-recorded allocation is stored

- **GIVEN** person `E100` holds no allocation
- **WHEN** the coordinator records `E100` on `Atlas` from 2027-02-01 to 2027-04-30
- **THEN** the profile lists that allocation with project, start and end

#### Scenario: A confirmed allocation cannot be removed by hand

- **GIVEN** a person confirmed on an `Atlas` post, holding one allocation with source
  `confirmation`
- **WHEN** the coordinator tries to remove that allocation
- **THEN** the removal is refused with the release-first reason
- **AND** the assignment, the allocation and the post's remaining headcount are unchanged

### Requirement: Overlapping allocations are accepted at entry

The system SHALL store two hand-entered allocations for the same person whose ranges intersect,
without an error or a warning that blocks the save. Overlap SHALL be reported as a fact — the person is
occupied on those days — not as a data error, and the coordinator SHALL be able to correct
either record afterwards.

**Traces:** proposal step 3; REQ-001 §5, §6; S9.

#### Scenario: Two overlapping allocations coexist

- **GIVEN** `E100` holds an allocation on `Atlas` from 2027-01-01 to 2027-02-28
- **WHEN** the coordinator records `E100` on `Borealis` from 2027-02-01 to 2027-03-31
- **THEN** both allocations are stored and no validation error appears

### Requirement: An allocation makes a person unavailable for an intersecting post

For any post, a person SHALL be unavailable when at least one allocation of that person
intersects `[post.start, post.end]`, the intersection being inclusive on both ends: an
allocation ending on `post.start` and one starting on `post.end` both block the person. A post
whose range does not intersect any allocation SHALL stay open to the person.

**Traces:** proposal key rule "An allocation occupies its dates"; REQ-001 §6 G6, §7 S5, S9;
S5, S9.

#### Scenario: A boundary day still blocks

- **GIVEN** `E100` is allocated from 2026-12-01 to 2027-01-01
- **WHEN** a post runs from 2027-01-01 to 2027-03-31
- **THEN** `E100` is unavailable, with the reason that the person is already allocated

#### Scenario: A later post stays open

- **GIVEN** the same allocation ending 2027-01-01
- **WHEN** a post runs from 2027-02-01 to 2027-04-30
- **THEN** `E100` passes G6 and appears in the suitable list
