# staffing/candidate-matching — delta spec

Scope: eligibility gates, the fit score, the ranking and the explanation a coordinator can
defend in front of a lead.

## ADDED Requirements

### Requirement: Gates are evaluated in a fixed order

For a post, the system SHALL evaluate every person against these gates in order and SHALL
record the first failing gate as that person's single exclusion reason: G1 the person is
active; G2 the person's role equals the post's role by identity; G3 the person's job level
rank is at or above the post's required rank; G4 for every `required` skill row the person
holds the skill at or above the minimum band; G5 the person's travel willingness rank is at or
above the post's travel rank; G6 no allocation of the person intersects `[post.start,
post.end]`. A person failing any gate SHALL NOT appear in the ranked list. G2 SHALL NOT be
relaxed anywhere: the cross-role fallback is a separate, lower tier with its own gates X1 and
X2, and a person admitted there is never a same-role candidate.

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
- **THEN** the candidate is ranked, and the shortfall appears only as a scoring reason

#### Scenario: Role equality is by identity

- **GIVEN** a role whose name was never changed and a post created with it
- **WHEN** the role is deactivated and the post is searched
- **THEN** people still holding that role are gated by the same role and are not silently
  admitted or excluded

### Requirement: Eligible candidates receive a 0–100 fit score

The system SHALL score every eligible candidate from five weighted criteria: required-skill fit
35 points, bonus-skill coverage 15, level fit 20, schedule headroom 20 and travel fit 10. Per
`required` row the contribution is `0.7 + 0.3 × min(surplus, 2) / 2` of its share — meeting the
minimum earns 70 % of the row and the remaining 30 % rewards up to two bands of surplus — and
the criterion is the mean over those rows. Bonus coverage SHALL be the share of `bonus` rows met
at or above the target, times 15. Level fit SHALL be `20 − 5 × (person rank − required rank)`,
giving 20, 15, 10 or 5. Schedule headroom SHALL be 20 when no allocation touches the 30 days
before `post.start` or the 30 days after `post.end`, 10 when one side is touched, 0 when both.
Travel fit SHALL be 10 on an exact match, 7 when the person is one band more flexible and 5
when two. A criterion with no rows of its kind SHALL be dropped and the remaining weights
rescaled to 100. The score SHALL be rounded to one decimal, and the displayed per-criterion
contributions SHALL use the same 100-point scale as the score. A cross-role person SHALL be
scored on the same five criteria with the same weights; a `required` row they do not hold SHALL
contribute 0 rather than exclude them, and their score SHALL NOT be compared with a score from
the same-role section.

**Traces:** proposal step 5 and key rules "score weights" and "cross-role fallback is a lower
tier"; REQ-001 §6 score table, §6.1; S18, S38.

#### Scenario: A candidate who exactly meets every requirement

- **GIVEN** a post with one `required` skill at `Practitioner`, required job level `Senior`,
  travel `limited`, no `bonus` row and dates clear of every allocation
- **WHEN** a `Senior` candidate holds that skill at `Practitioner` and accepts `limited` travel
- **THEN** the required-skill contribution is 24.5, the bonus criterion is dropped, and the
  score is 87.6 rather than 100

#### Scenario: Every band above the requirement costs 5 points

- **GIVEN** the post above and a candidate at job level `Lead`
- **WHEN** the candidate is scored
- **THEN** the level contribution is 15 instead of 20

#### Scenario: Surplus is capped at two bands

- **GIVEN** a `required` skill at `Novice` held at `Expert` by the candidate
- **WHEN** the candidate is scored
- **THEN** the required-skill contribution is the full 35 rather than more

#### Scenario: A post without required skills rescaled to 100

- **GIVEN** a post with no `required` row and one `bonus` row
- **WHEN** two candidates are scored
- **THEN** their scores use the weights 15 / 20 / 20 / 10 rescaled to 100, and each score stays
  within 0–100

#### Scenario: An allocation near the dates reduces headroom

- **GIVEN** a candidate whose allocation ends 10 days before `post.start`
- **WHEN** the candidate is scored
- **THEN** the schedule contribution is 10

### Requirement: Ranking is deterministic and explained

The system SHALL order ranked candidates by score descending, breaking ties by the
required-skill fit contribution and then by name, and SHALL show for every row the score with
its per-criterion contribution and the gate results behind it. When the cross-role fallback is
open, its people SHALL form their own section below the same-role ranking, ordered by the same
two keys inside that section. No cross-role row MAY be ordered above a same-role row.

**Traces:** proposal step 5; REQ-001 §6 ranking note, §6.1; S2, S18, S34, S38.

#### Scenario: Ties are broken by required-skill fit, then name

- **GIVEN** two candidates with the same total score, the first holding a larger skill surplus
- **WHEN** the ranked list is shown
- **THEN** the candidate with the higher required-skill contribution is listed first
- **AND** two candidates equal on both keys are ordered by name

#### Scenario: Contributions add up to the score

- **GIVEN** a post with 后端开发 as `required` at `Practitioner` and 测试 as `bonus` at
  `Practitioner`, required job level `Senior`, travel `limited`, and no allocation nearby
- **WHEN** a `Senior` candidate holds both skills at `Practitioner` and accepts `limited` travel
- **THEN** the five contributions are 24.5 / 15 / 20 / 20 / 10 and they add up to the
  score 89.5

### Requirement: Exclusions are reported, never ranked

The system SHALL list excluded people separately from the ranked list, each with its first
failing gate, and SHALL show a count of exclusions per gate. A search with no eligible
candidate SHALL return an empty ranked list together with those counts, and SHALL NOT fail.
When the fallback is evaluated, the exclusion list SHALL also count the people rejected by X1
(level not above the requirement) and by X2 (no domain skill at `Practitioner` or above), so
an empty cross-role section is explained rather than silent.

**Traces:** proposal step 5; REQ-001 §6.1, §7 S8; S2, S8, S35, S36, S37.

#### Scenario: Nothing passes the gates

- **GIVEN** a post whose travel requirement is `full` and a pool where everyone accepts at most
  `limited`
- **WHEN** the post is searched
- **THEN** the ranked list is empty, the exclusion counts show the travel gate, and the
  coordinator can report which requirement blocks the post

### Requirement: The cross-role fallback opens only when the post role has nobody

The system SHALL evaluate the cross-role fallback for a post only when no person of the post's
role passes G1, G3, G4, G5 and G6, so the same-role ranked list is empty. As soon as one
same-role candidate is ranked, the system SHALL produce no fallback list at all. Inside the
fallback the system SHALL consider people of other roles and SHALL admit only those passing G1,
G5 and G6 plus both fallback gates: X1 the person's job level rank is at or above the post's
required rank + 1, and X2 the person holds at least one skill of the post role's skill domain at
`Practitioner` (rank 3) or above. X1 SHALL stand in for G3 and X2 SHALL stand in for G2 and G4,
so the post's `required` rows SHALL NOT gate a cross-role person. A post whose role has an empty
skill domain SHALL yield no cross-role person rather than degrade to "any skill". Cross-role
people SHALL appear in a separate section below the same-role ranking, never interleaved with
it and never ordered above a same-role candidate, and every row SHALL show the person's role,
the number of bands above the requirement, the qualifying domain skill with its band, and the
score with its per-criterion contributions. The requirement's role SHALL never be added to the
person: the fallback is staffing advice for one post, not a second role.

**Traces:** proposal step 5 and key rule "cross-role fallback is a lower tier";
REQ-001 §6.1, §7 S33–S38; S33, S34, S35, S36, S37, S38.

#### Scenario: The fallback opens when the post role has nobody

- **GIVEN** a `Senior` `QA` post requiring 测试 at `Practitioner` and no `QA` person passing the
  gates
- **WHEN** the post is searched
- **THEN** the same-role ranked list is empty and a separate cross-role section is offered
- **AND** the coordinator is told the post role has no candidate as the reason the section exists

#### Scenario: One same-role candidate closes the fallback

- **GIVEN** the same post with one `QA` person passing every gate, while a `Lead` `DEV` holding
  测试 at `Practitioner` would also qualify
- **WHEN** the post is searched
- **THEN** only the `QA` person is ranked and no cross-role section is produced

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
lead, who sees only the recommendations made to their own posts, with score and reasons. The
full ranked list and the exclusion list SHALL NOT be reachable with a lead account.

**Traces:** proposal step 5 and step 8; REQ-001 §2, §8; S23.

#### Scenario: A lead cannot open the ranking

- **GIVEN** a lead owning the post
- **WHEN** the lead opens the post
- **THEN** only the recommended people are visible and no search action is offered
