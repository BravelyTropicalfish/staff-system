package com.example.staffing.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.staffing.data.PersonRepository;
import com.example.staffing.data.PostRepository;
import com.example.staffing.data.RecommendationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Proposing a person copies the row that admitted them. A cross-role admission is stored with the
 * fallback's own evidence, and a person off both lists is refused with the gate the exclusion list
 * already prints — or with the fact that the list has no entry for them at all. A snapshot that
 * cannot be written is a fault, never a proposal with an empty reason.
 */
class RecommendationServiceTest {

  private static final LocalDate START = LocalDate.parse("2027-02-01");
  private static final LocalDate END = LocalDate.parse("2027-06-30");
  private static final AuthenticatedUser COORDINATOR =
      new AuthenticatedUser(9, "coordinator", AuthenticatedUser.COORDINATOR, "Resource coordinator");

  private final PostRepository posts = mock(PostRepository.class);
  private final PersonRepository people = mock(PersonRepository.class);
  private final RecommendationRepository recommendations = mock(RecommendationRepository.class);
  private final MatchingService matching = mock(MatchingService.class);
  private final RecommendationService service =
      new RecommendationService(posts, people, recommendations, matching, new ObjectMapper());

  @Test
  void aCrossRoleAdmissionIsStoredWithTheFallbackEvidence() throws Exception {
    givenOpenPost();
    givenSearch(
        List.of(),
        List.of(new FallbackRow("E200", "Ada", "Frontend", "Lead", "none", 1, "SQL", "Proficient")),
        List.of());
    when(recommendations.pendingFor(1, "E200")).thenReturn(false);
    when(recommendations.insert(eq(1L), eq("E200"), anyString(), eq(9L))).thenReturn(5L);
    when(recommendations.findById(5L))
        .thenReturn(
            Optional.of(
                row(5, "{\"personNo\":\"E200\",\"gates\":[\"G1\",\"G5\",\"G6\",\"X1\",\"X2\"]}")));

    RecommendationView view = service.recommend(1, "E200", COORDINATOR);

    assertThat(view.id()).isEqualTo(5);
    ArgumentCaptor<String> written = ArgumentCaptor.forClass(String.class);
    verify(recommendations).insert(eq(1L), eq("E200"), written.capture(), eq(9L));
    RecommendationSnapshot stored =
        new ObjectMapper().readValue(written.getValue(), RecommendationSnapshot.class);
    assertThat(stored.crossRole())
        .isEqualTo(new RecommendationSnapshot.CrossRole(1, "SQL", "Proficient"));
    assertThat(stored.skills()).isEmpty();
  }

  @Test
  void aPersonOnNeitherListIsNotACandidate() {
    givenOpenPost();
    givenSearch(
        List.of(),
        List.of(new FallbackRow("E201", "Bo", "Frontend", "Lead", "none", 1, "SQL", "Expert")),
        List.of(new ExclusionRow("E202", "Cy", "Frontend", "G6", "the allocation intersects")));
    when(people.findByEmployeeNo("E200")).thenReturn(Optional.of(person("E200")));

    assertThatThrownBy(() -> service.recommend(1, "E200", COORDINATOR))
        .isInstanceOf(RecommendationException.class)
        .hasMessage(
            "the person is not a candidate for DEV; the cross-role section is closed while a DEV"
                + " person is suitable");
  }

  @Test
  void aPersonTheExclusionListCarriesIsRefusedWithThatGate() {
    givenOpenPost();
    givenSearch(
        List.of(),
        List.of(),
        List.of(
            new ExclusionRow("E200", "Ada", "Frontend", "G6", "the allocation intersects the post")));

    assertThatThrownBy(() -> service.recommend(1, "E200", COORDINATOR))
        .isInstanceOfSatisfying(
            RecommendationException.class,
            error -> {
              assertThat(error.code()).isEqualTo("G6");
              assertThat(error).hasMessage("the allocation intersects the post");
            });
  }

  @Test
  void aPersonThePoolDoesNotKnowAtAllIsNotFound() {
    givenOpenPost();
    givenSearch(List.of(), List.of(), List.of());
    when(people.findByEmployeeNo("E999")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.recommend(1, "E999", COORDINATOR))
        .isInstanceOf(NoSuchElementException.class)
        .hasMessage("No person E999");
  }

  @Test
  void aSnapshotThatCannotBeWrittenFailsRatherThanStoringNothing() throws Exception {
    ObjectMapper broken = mock(ObjectMapper.class);
    when(broken.writeValueAsString(any()))
        .thenThrow(new JsonProcessingException("unserializable") {});
    RecommendationService failing =
        new RecommendationService(posts, people, recommendations, matching, broken);
    givenOpenPost();
    givenSearch(
        List.of(suitable("E200")), List.of(), List.of());
    when(recommendations.pendingFor(1, "E200")).thenReturn(false);

    assertThatThrownBy(() -> failing.recommend(1, "E200", COORDINATOR))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("the reason snapshot could not be written");
  }

  private void givenOpenPost() {
    when(posts.findById(1L))
        .thenReturn(
            Optional.of(
                new PostRepository.PostRow(
                    1, 10, "Atlas", "OPEN", "lead.one", "Backend", 5, "DEV", "Senior", 1, START,
                    END, "none", "open", 0)));
  }

  private void givenSearch(
      List<SuitabilityRow> suitable, List<FallbackRow> fallback, List<ExclusionRow> excluded) {
    when(matching.search(1))
        .thenReturn(
            new CandidateSearchView(
                suitable,
                excluded,
                Map.of("G1", 0, "G2", 0, "G3", 0, "G4", 0, "G5", 0, "G6", 0, "X1", 0, "X2", 0),
                fallback,
                suitable.isEmpty() ? "no DEV person passes the gates" : null));
  }

  private static SuitabilityRow suitable(String employeeNo) {
    return new SuitabilityRow(
        employeeNo,
        employeeNo,
        "DEV",
        "Senior",
        "none",
        List.of("G1", "G2", "G3", "G4", "G5", "G6"),
        List.of(new CandidateSkillView("SQL", "required", "Expert", null)));
  }

  private static RecommendationRepository.Row row(long id, String snapshotJson) {
    return new RecommendationRepository.Row(
        id,
        1,
        "E200",
        "E200",
        true,
        "proposed",
        null,
        snapshotJson,
        "coordinator",
        LocalDateTime.parse("2027-01-01T09:00"));
  }

  private static PersonRepository.PersonRow person(String employeeNo) {
    return new PersonRepository.PersonRow(
        employeeNo, employeeNo, null, 5, "DEV", "Senior", "none", true);
  }
}
