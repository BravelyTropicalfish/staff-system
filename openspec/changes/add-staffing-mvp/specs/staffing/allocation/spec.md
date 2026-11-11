# staffing/allocation — delta spec

Scope: projects and allocation records — the occupancy data the date gate reads.

## ADDED Requirements

### Requirement: Projects are maintained by the coordinator

The system SHALL store a project with name, lead, start date and end date, where the lead is
one identified user. The coordinator SHALL be able to create and edit a project; a lead SHALL
NOT create a project. A project whose end date precedes its start date SHALL be refused.

**Traces:** proposal step 3; REQ-001 §3, §5; S22, S24.

#### Scenario: A lead cannot create a project

- **GIVEN** a user holding only the lead role
- **WHEN** the project list is opened
- **THEN** no create action is offered and a create request is refused

#### Scenario: Inverted dates are refused

- **GIVEN** the project form
- **WHEN** the coordinator submits end date 2027-03-31 with start date 2027-06-01
- **THEN** the save is refused and nothing is stored

### Requirement: Project status is informational

The system SHALL let the coordinator move a project between `planned`, `running` and
`closed`. The status SHALL NOT gate matching or editing: an allocation on a closed project
stays editable and removable, and a closed project never changes a gate result.

**Traces:** REQ-001 §5, §6; proposal step 3.

#### Scenario: Closing a project blocks nothing

- **GIVEN** a project whose status is `closed` and one allocation on it
- **WHEN** the coordinator shortens that allocation and a post on the project is searched
- **THEN** both succeed and no gate result changes because of the status

### Requirement: Allocation records a person on a project for a date range

The system SHALL store an allocation as person × project × inclusive date range. It SHALL
create one on every confirmed assignment and SHALL let the coordinator record one by hand for
a person who is busy elsewhere. The coordinator SHALL be able to change or remove any
allocation at any time, including after the project's end date.

**Traces:** proposal step 3 and key rule "schedule conflict is an inclusive date-range
intersection"; REQ-001 §5, §6; S9, S19, S22.

#### Scenario: A hand-recorded allocation is stored

- **GIVEN** person `E100` has no allocation
- **WHEN** the coordinator records `E100` on `Atlas` from 2027-02-01 to 2027-04-30
- **THEN** the profile lists that allocation with project, start and end

#### Scenario: An allocation is editable and removable at any time

- **GIVEN** an allocation ending 2026-06-30 on a project that closed in 2026
- **WHEN** the coordinator shortens the range, then removes the allocation
- **THEN** both changes are accepted and the person's occupancy shrinks accordingly

### Requirement: Overlapping allocations are accepted at entry

The system SHALL store two allocations for the same person whose ranges intersect, without an
error or a warning that blocks the save. Overlap SHALL be reported as a fact — the person is
occupied — not as a data error.

**Traces:** proposal step 3; REQ-001 §6 note on overlaps; S9.

#### Scenario: Two overlapping allocations coexist

- **GIVEN** `E100` holds an allocation on `Atlas` from 2027-01-01 to 2027-02-28
- **WHEN** the coordinator records `E100` on `Borealis` from 2027-02-01 to 2027-03-31
- **THEN** both allocations are stored and no validation error appears

### Requirement: An allocation makes a person unavailable for an intersecting post

For any post, a person SHALL be unavailable when at least one allocation of that person
intersects `[post.start, post.end]`, the intersection being inclusive on both ends: an
allocation ending on `post.start` and one starting on `post.end` both block the person.

**Traces:** proposal key rule "schedule conflict is an inclusive date-range intersection";
REQ-001 §6 G6; S9.

#### Scenario: A boundary day still blocks

- **GIVEN** `E100` is allocated from 2026-12-01 to 2027-01-01
- **WHEN** a post runs from 2027-01-01 to 2027-03-31
- **THEN** `E100` is unavailable, with the reason that the person is already allocated

#### Scenario: A release frees the dates immediately

- **GIVEN** the allocation above has been removed
- **WHEN** the same post is searched again
- **THEN** `E100` passes the date gate
