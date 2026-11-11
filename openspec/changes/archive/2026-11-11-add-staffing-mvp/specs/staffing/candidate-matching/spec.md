# staffing/candidate-matching — delta spec

Scope: eligibility gates, the name-ordered suitable list, the reasons a person is on it or
kept off it, and the cross-role fallback tier.

## ADDED Requirements

### Requirement: Gates are evaluated in a fixed order

For a post, the system SHALL evaluate every person against these gates in order and SHALL
record the first failing gate as that person's single exclusion reason: G1 the person is
active; G2 the person's role equals the post's role by identity; G3 the person's job level
rank is at or above the post's required rank; G4 for every `required` skill row the person
holds the skill at or above the minimum band; G5 the person's travel willingness rank is at or
above the post's travel rank; G6 no allocation of the person intersects the post's inclusive
date range — an allocation ending on the post's start date and one starting on its end date
both block, while a post whose dates do not intersect any allocation stays open. A person
failing any gate SHALL NOT appear in the suitable list. G2
SHALL NOT be relaxed anywhere: the cross-role fallback is a separate, lower tier with its own
gates X1 and X2, and a person admitted there is never a same-role candidate.

**Traces:** proposal step 5; REQ-001 §6 gate table, §6.1; S2, S5, S9, S16, S18.

#### Scenario: The first failing gate is the reported reason

- **GIVEN** a person whose role equals the post's role, whose job level is below the required
  band, and whose 后端开发 band is also below the minimum
- **WHEN** the post is searched
- **THEN** the person is excluded with the job level reason, not the skill reason

#### Scenario: A bonus shortfall never excludes

- **GIVEN** a candidate holding 后端开发 at `Practitioner` and 测试 at `Novice`, on a post
  requiring 后端开发 at `Practitioner` and rating 测试 as `bonus` at `Practitioner`
- **WHEN** the post is searched
- **THEN** the candidate is listed, and the shortfall appears only as an informational row

#### Scenario: Role equality is by identity

- **GIVEN** a role whose name was never changed and a post created with it
- **WHEN** the role is deactivated and the post is searched
- **THEN** people still holding that role are gated by the same role and are not silently
  admitted or excluded

#### Scenario: Only an intersecting range blocks

- **GIVEN** a person allocated to `Atlas` from 2027-01-01 to 2027-03-31
- **WHEN** a post of `Borealis` running 2027-05-01 to 2027-07-31 is searched
- **THEN** the person passes G6 and appears in the suitable list
- **AND** a post running 2027-03-31 to 2027-05-31 excludes the person, because the ranges touch

### Requirement: Suitable people are listed by name, never scored

The system SHALL treat passing every gate as the whole definition of suitable and SHALL NOT
compute a fit score, a grade or any preference ordering. Every suitable person SHALL appear in
the list, ordered by name ascending and then by employee number when two names are equal, so
the same post and pool always produce the same order. Each row SHALL show the person's role,
job level and travel willingness, the gates passed, and the skills that qualify the person with
their bands — every `required` row with the band held, every `bonus` row with the band held and
the target. A `bonus` shortfall SHALL be shown as information only: it SHALL NOT exclude anyone
and SHALL NOT reorder anyone. The system SHALL NOT name, mark or sort for a best candidate; the
coordinator decides whom to recommend.

**Traces:** proposal step 5 and key rule "No score, no ranking"; REQ-001 §6 suitable list, §7
S2, S18; S2, S18, S34.

#### Scenario: Everyone who passes is listed, with no score

- **GIVEN** a post and three people who pass every gate
- **WHEN** the post is searched
- **THEN** all three appear in the list, and no row shows a score, a grade or a rank position

#### Scenario: A bonus shortfall is shown, not applied

- **GIVEN** a suitable candidate holding 后端开发 at `Practitioner` and 测试 at `Novice`, on a
  post requiring 后端开发 at `Practitioner` and rating 测试 as `bonus` at `Practitioner`
- **WHEN** the post is searched
- **THEN** the row shows 后端开发 at the required band and 测试 at the band held against the
  target, and the candidate stays in the list in name order

#### Scenario: The order comes from the name, not the fit

- **GIVEN** two suitable people, the first with a larger skill surplus and a later name
- **WHEN** the list is shown
- **THEN** the person with the earlier name is listed first
- **AND** two people with the same name are ordered by employee number

### Requirement: Exclusions are reported, never listed as suitable

The system SHALL list excluded people separately from the suitable list, each with its first
failing gate, and SHALL show a count of exclusions per gate. A search with no suitable
candidate SHALL return an empty suitable list together with those counts, and SHALL NOT fail.
When the fallback is evaluated, the exclusion list SHALL also count the people rejected by X1
(level not above the requirement) and by X2 (no domain skill at `Practitioner` or above), so
an empty cross-role section is explained rather than silent.

**Traces:** proposal step 5; REQ-001 §6.1, §7 S8; S2, S8, S35, S36, S37.

#### Scenario: Nothing passes the gates

- **GIVEN** a post whose travel requirement is `full` and a pool where everyone accepts at most
  `limited`
- **WHEN** the post is searched
- **THEN** the suitable list is empty, the exclusion counts show the travel gate, and the
  coordinator can report which requirement blocks the post

### Requirement: The cross-role fallback opens only when the post role has nobody

The system SHALL evaluate the cross-role fallback for a post only when no person of the post's
role is suitable — every person of that role fails at least one of G1, G3, G4, G5 or G6 — so
the same-role suitable list is empty. As soon as one
same-role candidate is suitable, the system SHALL produce no fallback list at all. Inside the
fallback the system SHALL consider people of other roles and SHALL admit only those passing G1,
G5 and G6 plus both fallback gates: X1 the person's job level rank is at or above the post's
required rank + 1, and X2 the person holds at least one skill of the post role's skill domain at
`Practitioner` (rank 3) or above. X1 SHALL stand in for G3 and X2 SHALL stand in for G2 and G4,
so the post's `required` rows SHALL NOT gate a cross-role person. A post whose role has an empty
skill domain SHALL yield no cross-role person rather than degrade to "any skill". Cross-role
people SHALL appear in a separate section below the same-role suitable list, ordered by name
like the main list, never interleaved with it, and every row SHALL show the person's role, the
number of bands above the requirement, and the qualifying domain skill with its band. The
requirement's role SHALL never be added to the
person: the fallback is staffing advice for one post, not a second role.

**Traces:** proposal step 5 and key rule "Cross-role fallback is a lower tier";
REQ-001 §6.1, §7 S33–S38; S33, S34, S35, S36, S37, S38.

#### Scenario: The fallback opens when the post role has nobody

- **GIVEN** a `Senior` `QA` post requiring 测试 at `Practitioner` and no `QA` person passing the
  gates
- **WHEN** the post is searched
- **THEN** the same-role suitable list is empty and a separate cross-role section is offered
- **AND** the coordinator is told the post role has no candidate as the reason the section exists

#### Scenario: One same-role candidate closes the fallback

- **GIVEN** the same post with one `QA` person passing every gate, while a `Lead` `DEV` holding
  测试 at `Practitioner` would also qualify
- **WHEN** the post is searched
- **THEN** only the `QA` person is listed and no cross-role section is produced

#### Scenario: A person at the required band is not above it

- **GIVEN** the post role has nobody and a person of another role sits at the post's own required
  job level
- **WHEN** the fallback is evaluated
- **THEN** the person is excluded with the reason "cross-role: level not above the requirement"
- **AND** only people at least one band higher are admitted

#### Scenario: A skill outside the post role's domain never admits

- **GIVEN** a `QA` post, whose domain is 测试, and a `Lead` `PM` holding 项目管理 at `Expert` but
  no 测试 skill
- **WHEN** the fallback is evaluated
- **THEN** the person is excluded with the reason "cross-role: no domain skill at `Practitioner`"

#### Scenario: The top band has no cross-role candidate

- **GIVEN** a post requiring job level `Principal` and no `Principal` person of its role passing
  the gates
- **WHEN** the fallback is evaluated
- **THEN** the cross-role section is empty and the exclusion list states that no band sits above
  the requirement

#### Scenario: Cross-role evidence travels with the recommendation

- **GIVEN** a `QA` post with nobody and a `Lead` `DEV` holding 测试 at `Practitioner`
- **WHEN** the coordinator recommends that person and the lead opens the recommendation
- **THEN** the person appears only in the cross-role section, with the one-band gap and the
  qualifying skill shown
- **AND** the recommendation snapshot carries the same evidence, while confirmation, allocation
  and voiding behave exactly as for a same-role candidate

### Requirement: Only the coordinator searches the pool

The system SHALL grant candidate search to the coordinator for any post and SHALL deny it to a
lead, who sees only the recommendations made to their own posts, with their reason snapshot. The
full suitable list and the exclusion list SHALL NOT be reachable with a lead account.

**Traces:** proposal step 5 and step 8; REQ-001 §2, §8; S23.

#### Scenario: A lead cannot open the suitable list

- **GIVEN** a lead owning the post
- **WHEN** the lead opens the post
- **THEN** only the recommended people are visible and no search action is offered
