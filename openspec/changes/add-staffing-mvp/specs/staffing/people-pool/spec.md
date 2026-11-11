# staffing/people-pool — delta spec

Scope: the role and skill catalogs, the person master record and its proficiency bands.
Person data is written only by `staffing/people-import`; this capability owns the shapes
and the read surfaces.

## ADDED Requirements

### Requirement: Role catalog

The system SHALL maintain a role catalog whose entries carry a stable identity, a name and
an `active` flag, seeded with exactly five roles: `PM`, `BA`, `DEV`, `QA` and `UX`. A role
created later SHALL be usable by posts, people and the import exactly like a seeded role.

**Traces:** proposal step 1; REQ-001 §3, §5; S31.

#### Scenario: Seeded roles are the only choices at first use

- **GIVEN** a database with no user data
- **WHEN** the coordinator opens the role picker of a new post
- **THEN** exactly five active entries are offered: `PM`, `BA`, `DEV`, `QA`, `UX`

#### Scenario: A new role joins the catalog

- **GIVEN** the coordinator adds the role `SRE`
- **WHEN** the post form is opened again
- **THEN** `SRE` is offered, and an import row whose `role` cell reads `SRE` is accepted

### Requirement: Skill catalog

The system SHALL maintain a skill catalog whose entries carry a stable identity, a name, the
single role whose skill domain the skill belongs to, and an `active` flag, seeded with 后端开发,
前端开发, 项目管理, 业务分析, 测试 and UI设计.

**Traces:** proposal step 1 and key rule "every skill belongs to exactly one role domain";
REQ-001 §3, §5, §6 catalog lifecycle; S28.

#### Scenario: Skill picker offers the seeded set

- **GIVEN** the seeded catalogs and no user-added skill
- **WHEN** the lead opens the skill picker of a post
- **THEN** the six seeded skills are offered and nothing else

### Requirement: Every skill belongs to exactly one role domain

The system SHALL store on each skill exactly one role domain — the role whose skill domain
holds it — and SHALL NOT place one skill under two roles. The seeded mapping SHALL be `DEV` →
后端开发, 前端开发; `QA` → 测试; `BA` → 业务分析; `PM` → 项目管理; `UX` → UI设计. A role
created later SHALL start with an empty domain until the coordinator assigns skills to it. A
domain change SHALL be allowed even while the skill is referenced, SHALL apply to future
candidate searches only, and SHALL NOT rewrite any stored person, post, recommendation or
snapshot. The domain SHALL feed the cross-role fallback only: it SHALL NOT widen G4, and holding
a skill never grants a person a second role.

**Traces:** proposal key rules "every skill belongs to exactly one role domain" and "cross-role
fallback is a lower tier"; REQ-001 §5 vocabulary (role skill domain), §6 catalog lifecycle, §6.1.

#### Scenario: The seeded mapping is readable per role

- **GIVEN** the seeded catalogs and no user change
- **WHEN** the coordinator reads the domains of the five roles
- **THEN** `DEV` lists 后端开发 and 前端开发, `QA` lists 测试, `BA` lists 业务分析, `PM` lists
  项目管理 and `UX` lists UI设计
- **AND** every seeded skill appears under exactly one role

#### Scenario: Holding a skill grants no second role

- **GIVEN** person `E100` is a `DEV` holding 测试 at `Expert`
- **WHEN** the profile is read and a post of role `QA` is searched
- **THEN** `E100` still holds exactly one role, `DEV`, and is absent from the `QA` ranked list
- **AND** `E100` can surface for that post only through the cross-role section

#### Scenario: A role added later has an empty domain

- **GIVEN** the coordinator adds the role `SRE` and assigns no skill to it
- **WHEN** an `SRE` post has no `SRE` candidate and the fallback is evaluated
- **THEN** the cross-role section is empty, and no skill of any other role is treated as `SRE`'s
  domain

#### Scenario: A domain change reuses stored data untouched

- **GIVEN** 测试 belongs to the `QA` domain and a recommendation snapshot was taken under it
- **WHEN** the coordinator moves 测试 to another role's domain
- **THEN** the stored person, post and snapshot keep their existing data
- **AND** only searches made afterwards use the new domain

### Requirement: Referenced catalog items freeze and are never deleted

While at least one person or one post references a role or a skill, the system SHALL refuse
to rename it and SHALL refuse to delete it, and SHALL offer deactivation instead. A name
SHALL be editable while nothing references it. Deactivation and reactivation SHALL NOT
rewrite any stored reference: existing people, posts and recommendation reasons keep showing
the item, while new posts and new imports SHALL NOT select it. Maintaining the catalogs —
adding, renaming, deactivating and reactivating — SHALL be offered to the coordinator only.

**Traces:** proposal step 1 and key rule "catalog items are deactivated, never deleted";
REQ-001 §6 catalog lifecycle; S28.

#### Scenario: Delete and rename are both refused on a referenced skill

- **GIVEN** 后端开发 is referenced by 40 people and 3 posts
- **WHEN** the coordinator asks to delete it
- **THEN** the action is refused and the catalog still holds 后端开发
- **WHEN** the coordinator asks to rename it
- **THEN** the rename is refused

#### Scenario: Deactivation hides an item from new work only

- **GIVEN** 后端开发 has been deactivated
- **WHEN** the lead opens the skill picker of a new post
- **THEN** 后端开发 is absent from the picker
- **AND** a post that already requires it still lists it as `required`
- **AND** a person who already holds it still shows the band on the profile

#### Scenario: Rename is allowed before the first reference

- **GIVEN** a skill referenced by nobody
- **WHEN** the coordinator renames it
- **THEN** the rename succeeds and the catalog shows the new name

### Requirement: Person master record is keyed by employee number

The system SHALL store one person per employee number, holding name, optional email, role
reference, job level band, travel willingness and an `active` flag. The role SHALL be exactly
one catalog role, held as a single reference: a person never holds two roles, and no path in
the system MAY add a second one. The system SHALL NOT offer any in-app path that creates or
changes a person's master data; every such change SHALL originate from an import batch.

**Traces:** proposal steps 1–2 and key rule "person master data comes from a report";
REQ-001 §3, §5.1, §9; S13, S17.

#### Scenario: A person holds exactly one role

- **GIVEN** a person exists in the pool
- **WHEN** any person profile is read through the UI or the data model
- **THEN** the person holds exactly one role, and no control or field offers a second one
- **AND** no action anywhere in the system adds a role without replacing the existing one

#### Scenario: The profile is read-only

- **GIVEN** a person exists with job level `Junior`
- **WHEN** the coordinator opens the person profile
- **THEN** the master data is shown
- **AND** no control offers to save a change to name, email, role, job level, travel
  willingness or the active flag

### Requirement: Three ordered band scales

The system SHALL compare the three scales by band rank, never by a free number: job level
`Junior` < `Senior` < `Lead` < `Principal`, proficiency `Novice` < `Advanced beginner` <
`Practitioner` < `Proficient` < `Expert`, travel willingness `none` < `limited` < `full`.
The system SHALL display the band name wherever a band is shown.

**Traces:** proposal key rule "three ordered band scales";
REQ-001 §5 vocabulary, B18.

#### Scenario: A band is displayed, never a number

- **GIVEN** a person with proficiency `Practitioner` in 测试
- **WHEN** the coordinator opens the candidate search result
- **THEN** the row reads `Practitioner` and shows no numeric level

#### Scenario: Rank ordering drives comparisons

- **GIVEN** post requirement `Senior` and two people at `Junior` and `Lead`
- **WHEN** the gates are evaluated
- **THEN** the `Lead` person passes the level gate and the `Junior` person is excluded

### Requirement: One proficiency band per person and skill

The system SHALL hold at most one band per person-skill pair. An import SHALL insert or
overwrite that band and SHALL NOT delete one.

**Traces:** proposal step 2; REQ-001 §5, §5.1; S11, S15; D6.

#### Scenario: A band is overwritten, not duplicated

- **GIVEN** person `E100` holds 后端开发 at `Novice`
- **WHEN** an applied batch reports 后端开发 at `Practitioner` for `E100`
- **THEN** the profile shows one row for 后端开发, at `Practitioner`

#### Scenario: An omitted skill keeps its band

- **GIVEN** person `E100` holds 测试 at `Proficient`
- **WHEN** an applied batch lists `E100` with 后端开发 only
- **THEN** 测试 is still `Proficient`

### Requirement: Person profile is readable by the coordinator

The system SHALL show, for one person, the master data, every skill with its band, and every
allocation whose end date is today or later, with project and dates.

**Traces:** REQ-001 §8 display contract; proposal step 8.

#### Scenario: Profile lists a person's occupancy

- **GIVEN** `E100` has an allocation on project `Atlas` from 2027-01-01 to 2027-03-31
- **WHEN** the coordinator opens the profile on 2026-12-01
- **THEN** the profile lists that allocation with project, start and end
