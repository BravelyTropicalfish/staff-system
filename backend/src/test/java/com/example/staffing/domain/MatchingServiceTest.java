package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.SkillRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * The cross-role fallback: the tier opens only when no person of the post's role is suitable, and it
 * admits on G1, G5, G6, X1 and X2 instead of the post's required rows. Each refusal answers with the
 * gate that kept the person out, and the qualifying skill is the highest domain skill held at
 * `Practitioner` or above, ties broken by the smaller name so the list never depends on row order.
 */
class MatchingServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-02-01");
  private static final LocalDate END = LocalDate.parse("2027-06-30");

  private final PostRepository posts = mock(PostRepository.class);
  private final PersonRepository people = mock(PersonRepository.class);
  private final SkillRepository skills = mock(SkillRepository.class);
  private final MatchingService service = new MatchingService(posts, people, skills);

  @Test
  void aCrossRolePersonAboveTheLevelHoldingADomainSkillIsOffered() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenBands(new PersonRepository.PersonBandRow("E200", 7, "Proficient"));
    givenDomain(skill(7, "SQL"));

    CandidateSearchView view = service.search(1);

    assertThat(view.suitable()).isEmpty();
    assertThat(view.fallbackReason()).isEqualTo("no DEV person passes the gates");
    assertThat(view.fallback()).hasSize(1);
    FallbackRow row = view.fallback().get(0);
    assertThat(row.employeeNo()).isEqualTo("E200");
    assertThat(row.role()).isEqualTo("Frontend");
    assertThat(row.bandsAbove()).isEqualTo(1);
    assertThat(row.skill()).isEqualTo("SQL");
    assertThat(row.band()).isEqualTo("Proficient");
  }

  @Test
  void anInactiveCrossRolePersonIsExcludedUnderG1() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", false));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.fallback()).isEmpty();
    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("G1");
      assertThat(row.reason()).isEqualTo("the person is not active");
    });
  }

  @Test
  void aTravelWillingnessBelowTheRequirementIsExcludedUnderG5() {
    givenPost("Senior", "full");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement()
        .satisfies(row -> assertThat(row.gate()).isEqualTo("G5"));
  }

  @Test
  void anAllocationThatIntersectsThePostIsExcludedUnderG6() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenAllocations(new PersonRepository.PersonRangeRow("E200", START.plusDays(10), END));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("G6");
      assertThat(row.reason()).contains(START.toString()).contains(END.toString());
    });
  }

  @Test
  void anAllocationThatEndsBeforeThePostDoesNotGateThePerson() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenAllocations(
        new PersonRepository.PersonRangeRow("E200", START.minusDays(20), START.minusDays(10)));
    givenBands(new PersonRepository.PersonBandRow("E200", 7, "Practitioner"));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).isEmpty();
    assertThat(view.fallback()).singleElement()
        .satisfies(row -> assertThat(row.employeeNo()).isEqualTo("E200"));
  }

  @Test
  void aLevelNotAboveTheRequirementIsExcludedUnderX1() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Senior", "none", true));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("X1");
      assertThat(row.reason()).isEqualTo("cross-role: level not above the requirement");
    });
  }

  @Test
  void aRequirementAtTheTopBandNamesTheMissingBandAboveIt() {
    givenPost("Principal", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Principal", "none", true));
    givenDomainskill();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("X1");
      assertThat(row.reason()).isEqualTo("cross-role: no band sits above Principal");
    });
  }

  @Test
  void aRoleWithNoDomainSkillAdmitsNobodyUnderX2() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenDomain();

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("X2");
      assertThat(row.reason()).isEqualTo("cross-role: DEV has no skill to qualify on");
    });
  }

  @Test
  void aDomainSkillBelowPractitionerDoesNotQualifyUnderX2() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenBands(new PersonRepository.PersonBandRow("E200", 7, "Novice"));
    givenDomain(skill(7, "SQL"));

    CandidateSearchView view = service.search(1);

    assertThat(view.excluded()).singleElement().satisfies(row -> {
      assertThat(row.gate()).isEqualTo("X2");
      assertThat(row.reason()).isEqualTo("cross-role: no domain skill at Practitioner");
    });
  }

  @Test
  void theHighestQualifyingDomainSkillWinsAndTiesGoToTheSmallerName() {
    givenPost("Senior", "none");
    givenEveryone(person("E200", "Ada", "Frontend", 4, "Lead", "none", true));
    givenBands(
        new PersonRepository.PersonBandRow("E200", 7, "Proficient"),
        new PersonRepository.PersonBandRow("E200", 8, "Expert"),
        new PersonRepository.PersonBandRow("E200", 9, "Expert"),
        new PersonRepository.PersonBandRow("E200", 10, "Expert"),
        new PersonRepository.PersonBandRow("E200", 12, "Proficient"));
    givenDomain(skill(7, "SQL"), skill(8, "Kotlin"), skill(9, "Access"), skill(10, "Rust"),
        skill(11, "Go"), skill(12, "Zeta"));

    CandidateSearchView view = service.search(1);

    assertThat(view.fallback()).singleElement().satisfies(row -> {
      assertThat(row.skill()).isEqualTo("Access");
      assertThat(row.band()).isEqualTo("Expert");
    });
  }

  private void givenPost(String minJobLevel, String travel) {
    when(posts.findById(1L))
        .thenReturn(
            Optional.of(
                new PostRepository.PostRow(
                    1, 10, "Atlas", "OPEN", "lead.one", null, 5, "DEV", minJobLevel, 1, START, END,
                    travel, "open", 0)));
    when(posts.skillsOf(1L)).thenReturn(List.of());
  }

  private void givenEveryone(PersonRepository.PersonRow... rows) {
    when(people.findAll()).thenReturn(List.of(rows));
  }

  private void givenBands(PersonRepository.PersonBandRow... rows) {
    when(people.allBands()).thenReturn(List.of(rows));
  }

  private void givenAllocations(PersonRepository.PersonRangeRow... rows) {
    when(people.allAllocations()).thenReturn(List.of(rows));
  }

  private void givenDomain(SkillRepository.SkillRow... rows) {
    when(skills.findByDomain(5L)).thenReturn(List.of(rows));
  }

  /** The domain the admitted person qualifies on, so only the gate under test decides. */
  private void givenDomainskill() {
    givenDomain(skill(7, "SQL"));
  }

  private static SkillRepository.SkillRow skill(long id, String name) {
    return new SkillRepository.SkillRow(id, name, 5, "DEV", true);
  }

  private static PersonRepository.PersonRow person(
      String employeeNo,
      String name,
      String role,
      long roleId,
      String jobLevel,
      String travel,
      boolean active) {
    return new PersonRepository.PersonRow(
        employeeNo, name, null, roleId, role, jobLevel, travel, active);
  }
}
