# staffing/staffing-confirmation — delta spec

Scope: the handshake — recommend, confirm, reject, withdraw, release — and the shared view both
roles end up looking at.

## ADDED Requirements

### Requirement: The coordinator recommends a ranked candidate

The system SHALL let the coordinator recommend a candidate for a post that still has a
remaining slot, and SHALL refuse the action when the post is `filled`, `cancelled` or the
person no longer passes the gates. A recommendation SHALL be stored as `proposed` with an
immutable snapshot of the score, the per-criterion contributions and the reasons at
recommendation time, plus who proposed it and when. A candidate admitted through the cross-role
fallback SHALL be recommendable on the same terms, and its snapshot SHALL also carry the
evidence that admitted them: the person's role, the number of job level bands above the post
requirement and the qualifying domain skill with its band.

**Traces:** proposal step 6 and key rule "cross-role fallback is a lower tier";
REQ-001 §5, §6, §6.1, §7 S3; S3, S38.

#### Scenario: A recommendation carries its snapshot

- **GIVEN** a ranked candidate scoring 84.5 on a post with one remaining slot
- **WHEN** the coordinator recommends the candidate
- **THEN** the recommendation is `proposed` for the lead and shows 84.5 with its reasons taken
  at that moment

#### Scenario: A filled post accepts no recommendation

- **GIVEN** a `filled` post
- **WHEN** the coordinator recommends a candidate
- **THEN** the action is refused and no recommendation is stored

#### Scenario: A cross-role recommendation carries its evidence

- **GIVEN** a `QA` post whose role has nobody and an open cross-role section holding a `Lead`
  `DEV` with 测试 at `Practitioner`
- **WHEN** the coordinator recommends that person
- **THEN** the recommendation is `proposed` with the score snapshot plus the person's role, the
  one-band gap above the requirement and the qualifying skill
- **AND** confirming it applies G1 and G6 only, writes the same assignment and allocation, and
  voids competing recommendations exactly as for a same-role candidate

### Requirement: One person may be pending on several posts

The system SHALL allow the same person to hold a `proposed` recommendation on more than one
post at the same time; proximity of the posts shall not block a proposal. The conflict SHALL be
resolved at confirmation, when the other proposals are voided.

**Traces:** proposal step 6; REQ-001 §6; S19.

#### Scenario: Two proposals for one person

- **GIVEN** posts `A` and `B` with overlapping dates
- **WHEN** the coordinator recommends `E100` on both
- **THEN** two `proposed` recommendations exist

### Requirement: Only the owning lead confirms, and the gates are re-checked

The system SHALL let the lead of the post's project confirm a recommendation and SHALL refuse
it for any other user. Before committing, the system SHALL re-run G1 and G6; if either fails,
the confirmation SHALL be refused with that reason and the recommendation SHALL stay
`proposed`.

**Traces:** proposal step 6 and confirmation write-back; REQ-001 §2, §6; S10, S22, S24.

#### Scenario: A person deactivated after the proposal

- **GIVEN** a `proposed` recommendation whose person the report has since marked inactive
- **WHEN** the lead confirms it
- **THEN** the confirmation is refused with the active-status reason and the recommendation
  stays `proposed`

#### Scenario: An allocation recorded after the proposal

- **GIVEN** a `proposed` recommendation and an allocation overlapping the post dates
- **WHEN** the lead confirms it
- **THEN** the confirmation is refused with the schedule reason and the recommendation stays
  `proposed`

#### Scenario: Another lead's post is out of reach

- **GIVEN** a recommendation on a post of a project the lead does not own
- **WHEN** the lead confirms it
- **THEN** the action is refused and no assignment or allocation is written

### Requirement: Confirmation writes the assignment back

On a successful confirmation the system SHALL apply every effect below together, or none of
them: mark the recommendation `confirmed`; create an assignment for the post and person;
create an allocation for the person, the project and the post's date range; decrement the
remaining headcount, setting the post to `filled` at zero; and void every other pending
recommendation of that person with reason `person taken`.

**Traces:** proposal steps 6–7 and key rule "a confirmed person is written back as an
allocation"; REQ-001 §6 confirmation write-back; S4, S5, S7, S19.

#### Scenario: Confirmation is visible to both roles

- **GIVEN** a `proposed` recommendation on an `open` post
- **WHEN** the lead confirms it
- **THEN** the person appears under the post's chosen people for both roles
- **AND** the remaining headcount drops by one
- **AND** the person shows an allocation for the post dates

#### Scenario: The confirmed person leaves other searches

- **GIVEN** the person just confirmed
- **WHEN** another post with overlapping dates is searched
- **THEN** the person is excluded with the schedule reason

#### Scenario: The other pending proposal is voided

- **GIVEN** `E100` proposed on posts `A` and `B`, both `proposed`
- **WHEN** the lead of `A` confirms
- **THEN** the recommendation on `B` becomes `voided` with reason `person taken`
- **AND** the coordinator sees it in the notice list

### Requirement: No double booking

The system SHALL hold at most one active assignment per post and person, and SHALL NOT let two
active assignments of one person carry intersecting date ranges — a person may hold two
assignments only when the ranges do not overlap. When two confirmations for the same person
compete, the first to commit SHALL win and the second SHALL be refused with the person-taken
reason instead of producing a double booking.

**Traces:** proposal "Why" item 1 (double-booking) and key rule "one person may be
recommended for several posts"; REQ-001 §6.

#### Scenario: A concurrent double confirmation

- **GIVEN** two leads confirming the same person on overlapping posts
- **WHEN** both requests arrive together
- **THEN** exactly one assignment exists and the other confirmation is refused with the
  person-taken reason

#### Scenario: Sequential postings stay allowed

- **GIVEN** a person whose confirmed assignment ends 2027-03-31
- **WHEN** a post running from 2027-04-01 to 2027-06-30 is searched and confirmed
- **THEN** the confirmation succeeds and the person holds two non-overlapping assignments

### Requirement: Rejecting or withdrawing frees the person

The system SHALL let the lead reject a `proposed` recommendation, storing it as `rejected` with
an optional note, and SHALL let the coordinator withdraw their own `proposed` recommendation,
storing it as `withdrawn`. In both cases the person SHALL become available to other posts and
the coordinator SHALL be able to recommend another candidate.

**Traces:** proposal step 6; REQ-001 §5, §6, §7 S6; S6.

#### Scenario: A rejection frees the candidate

- **GIVEN** a `proposed` recommendation
- **WHEN** the lead rejects it with a note
- **THEN** the recommendation is `rejected` and the person is available again

#### Scenario: The coordinator withdraws a proposal

- **GIVEN** a `proposed` recommendation the coordinator no longer wants
- **WHEN** the coordinator withdraws it
- **THEN** the recommendation is `withdrawn` and no assignment exists

### Requirement: Releasing a person reopens the slot

The system SHALL let the lead or the coordinator release a confirmed person. Releasing SHALL
delete the assignment and its allocation, reopen the slot, recompute the post status as `open`
or `partially filled` from the remaining count, and make the person available for intersecting
posts.

**Traces:** proposal step 6; REQ-001 §3, §6; S21.

#### Scenario: Release returns the post to the short list

- **GIVEN** a `filled` post with one confirmed person
- **WHEN** that person is released
- **THEN** the assignment and its allocation are gone
- **AND** the post is `open` with remaining headcount 1 and appears in the "still short" list
- **AND** the person passes the date gate for overlapping posts

### Requirement: Every void carries a reason and reaches the coordinator

The system SHALL void a pending recommendation with exactly one of these reasons — `person
taken` on confirmation of another post, `post changed` on an editable post change, `post
cancelled` on cancellation — and SHALL list each voided recommendation in a notice list the
coordinator can read, showing post, person, reason and time. An entry SHALL remain in the list
until the coordinator dismisses it, and SHALL NOT expire on its own.

**Traces:** proposal step 6 and REQ-001 §6 write-back step 6; REQ-001 §5, §6; S19, S25,
S21.

#### Scenario: The notice list explains each loss

- **GIVEN** three recommendations voided for the three different reasons
- **WHEN** the coordinator opens the notice list
- **THEN** each entry shows its post, person, reason and time

#### Scenario: An entry stays until it is dismissed

- **GIVEN** a voided recommendation listed 90 days ago and never dismissed
- **WHEN** the coordinator opens the notice list
- **THEN** the entry is still listed with its original reason and time

### Requirement: Both roles share one view of who is staffed and what is missing

The system SHALL show, per post, the chosen people with their confirmation date and the
remaining headcount, identically to the lead and the coordinator. A lead SHALL see only the
projects they own; the coordinator SHALL see all projects and, additionally, for every
recommendation the score, the reasons and the decision state.

**Traces:** proposal step 8; REQ-001 §2, §8; S4, S23.

#### Scenario: Both roles see the same gap numbers

- **GIVEN** a post with headcount 2 and one confirmed person
- **WHEN** the lead and the coordinator each open the project
- **THEN** both see the same chosen person and the same remaining headcount 1
- **AND** only the coordinator sees the recommendation score and reasons
