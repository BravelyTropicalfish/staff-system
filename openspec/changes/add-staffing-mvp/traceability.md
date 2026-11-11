# Scenario traceability (T27)

Every scenario in the six delta specs resolves to at least one test. The **layer** column is the
primary layer the ATDD design assigned (82 API, 8 E2E); a **companion** is an extra check of the same
scenario that adds no scenario row, exactly as `atdd-design.md` §1 describes. API ids are
`Class#method` under `backend/src/test/java/com/example/staffing/`; E2E ids are the test title inside
`web/e2e/scenarios.spec.ts` or `web/e2e/journeys.spec.ts`.

Totals: 90 scenarios = 82 API + 8 E2E. Rows: allocation 11, candidate-matching 15, people-import 15,
people-pool 17, post-requirement 14, staffing-confirmation 18.

## allocation (11)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| A lead cannot create a project | API | `ProjectApiTest#aLeadCannotCreateAProject` | — |
| Inverted dates are refused | API | `ProjectApiTest#invertedDatesAreRefusedAndNothingIsStored` | — |
| The end releases everyone and cancels the posts | E2E | `scenarios.spec.ts` — The end releases everyone and cancels the posts | `ProjectApiTest#endingReleasesEveryoneAndCancelsThePosts` |
| Only the project's lead may end it | API | `ProjectApiTest#onlyTheProjectsLeadMayEndIt` | — |
| A closed project cannot be reopened | API | `ProjectApiTest#aClosedProjectCannotBeEndedAgainOrReopened` | — |
| Confirmation writes one allocation for the post's dates | API | `ConfirmationApiTest#confirmationWritesOneAllocationForThePostsDates` | — |
| A hand-recorded allocation is stored | API | `AllocationApiTest#aHandRecordedAllocationIsStored` | — |
| A confirmed allocation cannot be removed by hand | API | `AllocationApiTest#aConfirmedAllocationCannotBeRemovedOrEditedByHand` | — |
| Two overlapping allocations coexist | API | `AllocationApiTest#twoOverlappingAllocationsCoexist` | — |
| A boundary day still blocks | API | `RangesTest#aBoundaryDayStillBlocks` | `GatesTest#anAllocationEndingOnThePostStartBlocks` |
| A later post stays open | API | `RangesTest#aLaterPostStaysOpen` | `GatesTest#aPostAfterTheAllocationStaysOpen` |

## candidate-matching (15)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| The first failing gate is the reported reason | API | `CandidateApiTest#theFirstFailingGateIsTheReportedReason` | `GatesTest#theFirstFailingGateIsTheReportedReason` |
| A bonus shortfall never excludes | API | `CandidateApiTest#aBonusShortfallNeverExcludes` | — |
| Role equality is by identity | API | `CandidateApiTest#roleEqualityIsByTheIdentity` | `GatesTest#roleEqualityIsTheRoleRowNotItsName` |
| Only an intersecting range blocks | API | `CandidateApiTest#onlyAnIntersectingRangeBlocks` | — |
| Everyone who passes is listed, with no score | API | `CandidateApiTest#everyoneWhoPassesIsListedWithNoScore` | — |
| A bonus shortfall is shown, not applied | API | `CandidateApiTest#aBonusShortfallIsShownNotApplied` | — |
| The order comes from the name, not the fit | API | `CandidateApiTest#theOrderComesFromTheNameNotTheFit` | — |
| Nothing passes the gates | API | `CandidateApiTest#nothingPassesTheGates` | — |
| The fallback opens when the post role has nobody | API | `CrossRoleApiTest#theFallbackOpensWhenThePostRoleHasNobody` | — |
| One same-role candidate closes the fallback | API | `CrossRoleApiTest#oneSameRoleCandidateClosesTheFallback` | — |
| A person at the required band is not above it | API | `CrossRoleApiTest#aPersonAtTheRequiredBandIsNotAboveIt` | — |
| A skill outside the post role's domain never admits | API | `CrossRoleApiTest#aSkillOutsideThePostRoleDomainNeverAdmits` | — |
| The top band has no cross-role candidate | API | `CrossRoleApiTest#theTopBandHasNoCrossRoleCandidate` | — |
| Cross-role evidence travels with the recommendation | API | `RecommendationApiTest#aCrossRoleRecommendationCarriesItsEvidence` | `CrossRoleApiTest#crossRoleEvidenceCarriesTheGapAndTheQualifyingSkill` |
| A lead cannot open the suitable list | API | `CandidateApiTest#aLeadCannotOpenTheSuitableList` | — |

## people-import (15)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| Skills-only refresh is accepted | API | `ImportApiTest#aSkillsOnlyRefreshIsAcceptedAndCreatesNoPerson` | — |
| A workbook with one sheet is refused | API | `ImportApiTest#aWorkbookWithOneSheetIsRefusedAndLeavesNoBatch` | `WorkbookParserTest#aWorkbookWithoutTheSkillsSheetIsRefused` |
| An empty workbook is refused | API | `ImportApiTest#anEmptyWorkbookIsRefusedByName` | `WorkbookParserTest#aWorkbookWithHeadersOnlyIsRefusedAsEmpty` |
| One bad row blocks the whole file | API | `ImportApiTest#oneBadRowBlocksTheWholeFile` | — |
| The corrected file is a new batch | API | `ImportApiTest#theCorrectedFileIsANewBatchWhileTheRejectedOneKeepsItsErrors` | — |
| A rejected batch stays readable | API | `ImportApiTest#aRejectedBatchStaysReadable` | — |
| Lower case is normalised, not rejected | API | `ImportApiTest#lowerCaseIsNormalisedNotRejected` | `ImportValidatorTest#lowerCaseIsNormalisedToTheCatalogSpelling` |
| A role outside the catalog blocks the file | API | `ImportApiTest#aRoleOutsideTheCatalogBlocksTheFileAndListsTheAcceptedRoles` | `ImportValidatorTest#aRoleOutsideTheCatalogBlocksTheFileAndNamesTheAcceptedRoles` |
| A multi-role cell is a row error, never two roles | API | `ImportApiTest#aMultiRoleCellIsARowErrorAndNeverTwoRoles` | `ImportValidatorTest#aMultiRoleCellIsOneRowErrorTwiceAndNeverTwoRoles` |
| A duplicated skill row blocks the file | API | `ImportApiTest#aDuplicatedSkillRowPutsBothRowsInTheHistory` | `ImportValidatorTest#aDuplicatedSkillRowPutsBothRowNumbersInTheErrorList` |
| An orphan skill row blocks the file | API | `ImportApiTest#anOrphanSkillRowSaysPersonNotFound` | `ImportValidatorTest#anOrphanSkillRowSaysPersonNotFound` |
| A duplicated employee number blocks the file | API | `ImportApiTest#aDuplicatedEmployeeNumberBlocksTheFile` | `ImportValidatorTest#aDuplicatedEmployeeNumberNamesTheNumber` |
| New and existing people in one file | API | `ImportApiTest#newAndExistingPeopleAreWrittenInOneFile` | — |
| An inactive person is reactivated by the report | API | `ImportApiTest#anInactivePersonIsReactivatedByTheReport` | — |
| Allocations survive an import | API | `ImportApiTest#allocationsSurviveAnImport` | — |

## people-pool (17)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| Seeded roles are the only choices at first use | API | `CatalogApiTest#theSeededCatalogsAreTheOnlyChoicesAtFirstUse` | `SchemaSeedTest#seedOffersExactlyTheFiveSeededRoles` |
| A new role joins the catalog | API | `CatalogApiTest#aRoleAddedLaterStartsWithAnEmptyDomain` | — |
| Skill picker offers the seeded set | API | `CatalogApiTest#theSeededCatalogsAreTheOnlyChoicesAtFirstUse` | `SchemaSeedTest#everySeededSkillHasExactlyOneRoleDomain` |
| The seeded mapping is readable per role | API | `CatalogApiTest#theSeededMappingIsReadablePerRole` | — |
| Holding a skill grants no second role | API | `PeopleApiTest#aPersonHoldsOneRoleAndASkillOfAnotherDomainAddsNoSecondOne` | — |
| A role added later has an empty domain | API | `CatalogApiTest#aRoleAddedLaterStartsWithAnEmptyDomain` | — |
| A domain change reuses stored data untouched | API | `CatalogApiTest#aDomainChangeReusesStoredDataUntouched` | — |
| Delete and rename are both refused on a referenced skill | API | `CatalogApiTest#deleteAndRenameAreBothRefusedOnAReferencedSkill` | — |
| Deactivation hides an item from new work only | API | `CatalogApiTest#deactivationHidesAnItemFromNewWorkOnly` | — |
| Rename is allowed before the first reference | API | `CatalogApiTest#renameIsAllowedBeforeTheFirstReference` | — |
| A person holds exactly one role | API | `PeopleApiTest#aPersonHoldsOneRoleAndASkillOfAnotherDomainAddsNoSecondOne` | `SchemaSeedTest#aPersonCarriesOneRoleByConstruction` |
| The profile is read-only | E2E | `scenarios.spec.ts` — The profile is read-only | `PeopleApiTest#theProfileAnswersNoWriteMethod` |
| A band is displayed, never a number | E2E | `scenarios.spec.ts` — A band is displayed, never a number | `PeopleApiTest#everyBandIsShownByNameAndNeverAsANumber` |
| Rank ordering drives comparisons | API | `BandsTest#aHigherJobLevelOutranksALowerOne` | `BandsTest#theProficiencyScaleRunsFromNoviceToExpert`, `BandsTest#travelWillingnessIsOrderedToo` |
| A band is overwritten, not duplicated | API | `PeopleApiTest#aBandIsOverwrittenInPlace` | — |
| An omitted skill keeps its band | API | `PeopleApiTest#anOmittedSkillKeepsItsBand` | — |
| Profile lists a person's occupancy | E2E | `scenarios.spec.ts` — Profile lists a person's occupancy | `PeopleApiTest#theProfileListsEveryAllocationAndMarksTheEndedOne` |

## post-requirement (14)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| A valid post is created open | API | `PostApiTest#aValidPostIsCreatedOpen` | — |
| A fractional headcount is refused | API | `PostApiTest#aFractionalHeadcountIsRefused` | — |
| A deactivated role does not block the title | API | `PostApiTest#aDeactivatedRoleDoesNotBlockTheTitle` | — |
| Required and bonus rows are distinguished | API | `PostApiTest#requiredAndBonusRowsAreDistinguished` | — |
| A post without skills is valid | API | `PostApiTest#aPostWithoutSkillsIsValid` | — |
| Filling the last slot | API | `PostApiTest#fillingTheLastSlotLeavesTheStillShortList` | `PostStatusTest#theWholeHeadcountIsFilled` |
| An inactive confirmed person keeps the post filled | API | `PostApiTest#anInactiveConfirmedPersonKeepsThePostFilled` | — |
| Dates cannot move under a confirmed person | API | `PostApiTest#datesCannotMoveUnderAConfirmedPerson` | — |
| The lock spreads to every gate-bearing field | API | `PostApiTest#theLockSpreadsToEveryGateBearingField` | — |
| Raising the headcount reopens a filled post | API | `PostApiTest#raisingTheHeadcountReopensAFilledPost` | — |
| Lowering below the confirmed count is refused | API | `PostApiTest#loweringBelowTheConfirmedCountIsRefused` | — |
| A bonus target change voids only pending recommendations | API | `PostApiTest#aBonusTargetChangeVoidsOnlyPendingRecommendations` | — |
| Cancel is refused, then allowed | API | `PostApiTest#cancelIsRefusedThenAllowed` | — |
| A coordinator cannot edit a post | API | `PostApiTest#aCoordinatorCannotEditAPost` | — |

## staffing-confirmation (18)

| Scenario | Layer | Test id | Companion |
|---|---|---|---|
| A recommendation carries its snapshot | API | `RecommendationApiTest#aRecommendationCarriesItsSnapshot` | — |
| A filled post accepts no recommendation | API | `RecommendationApiTest#aFilledPostAcceptsNoRecommendation` | — |
| A cross-role recommendation carries its evidence | API | `RecommendationApiTest#aCrossRoleRecommendationCarriesItsEvidence` | — |
| Two proposals for one person | API | `RecommendationApiTest#twoProposalsForOnePerson` | — |
| A person deactivated after the proposal | API | `ConfirmationApiTest#aPersonDeactivatedAfterTheProposal` | — |
| An allocation recorded after the proposal | API | `ConfirmationApiTest#anAllocationRecordedAfterTheProposal` | — |
| Another lead's post is out of reach | API | `ConfirmationApiTest#anotherLeadsPostIsOutOfReach` | — |
| Confirmation is visible to both roles | E2E | `scenarios.spec.ts` — Confirmation is visible to both roles | `ConfirmationApiTest#confirmationIsVisibleToBothRoles` |
| The confirmed person leaves other searches | E2E | `scenarios.spec.ts` — The confirmed person leaves other searches | `ConfirmationApiTest#theConfirmedPersonLeavesOtherSearches` |
| The other pending proposal is voided | API | `ConfirmationApiTest#theOtherPendingProposalIsVoided` | — |
| A concurrent double confirmation | API | `ConfirmationApiTest#aConcurrentDoubleConfirmation` | `IdempotencyApiTest#aConcurrentDoubleConfirmationLeavesOneAssignment` |
| A later posting succeeds, an overlapping one waits | API | `ConfirmationApiTest#aLaterPostingSucceedsAnOverlappingOneWaits` | — |
| A rejection frees the candidate | API | `ConfirmationApiTest#aRejectionFreesTheCandidate` | — |
| The coordinator withdraws a proposal | API | `ConfirmationApiTest#theCoordinatorWithdrawsAProposal` | — |
| Release returns the post to the short list | API | `ReleaseApiTest#releaseReturnsThePostToTheShortList` | — |
| The notice list explains each loss | E2E | `scenarios.spec.ts` — The notice list explains each loss | `NoticeApiTest#theNoticeListExplainsEachLoss` |
| An entry stays until it is dismissed | API | `NoticeApiTest#anEntryStaysUntilItIsDismissed` | — |
| Both roles see the same gap numbers | E2E | `scenarios.spec.ts` — Both roles see the same gap numbers | — |

## The two journeys

They add no scenario. `web/e2e/journeys.spec.ts` holds both, and each step that has a screen is driven
through it:

| Journey | Steps through a screen | Steps seeded through the API | Why |
|---|---|---|---|
| J1 import → project → post → search → recommend → confirm → gap | import (`/imports`), search (`/posts/:id/candidates`), confirm (`/posts/:id`), gap (`/projects/:id`) | create the project, create its post, propose the person | no screen owns project creation or the proposal; the routes used are the ones such a screen would call |
| J2 project end → everyone released → the same people in a later search | end (`/projects/:id`, twice), later search (`/posts/:id/candidates`) | the two projects, their posts, the proposal and its confirmation | same reason as J1 |

The proposal gap is the one product hole T27 found: the candidate screen is read-only and no screen
posts a recommendation, so a coordinator cannot propose from the UI yet. It is recorded in
`handoff.md` rather than papered over with an API-only assertion.
