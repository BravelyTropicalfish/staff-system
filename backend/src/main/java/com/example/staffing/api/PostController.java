package com.example.staffing.api;

import com.example.staffing.domain.PostDraft;
import com.example.staffing.domain.PostGapView;
import com.example.staffing.domain.PostService;
import com.example.staffing.domain.PostView;
import com.example.staffing.domain.SkillDraft;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Posts and the gap view. The project's lead owns every write; the coordinator may read the posts
 * and the gap but is refused every save and the cancel, so the screen is convenience and the server
 * is the authority.
 */
@RestController
public class PostController {

  private final PostService posts;

  public PostController(PostService posts) {
    this.posts = posts;
  }

  @GetMapping("/api/projects/{projectId}/posts")
  List<PostView> list(@PathVariable long projectId, Authentication authentication) {
    return posts.list(projectId, Auth.user(authentication));
  }

  /** The one gap board: every post the caller may read, with the project each one belongs to. */
  @GetMapping("/api/posts")
  List<PostView> listAll(Authentication authentication) {
    return posts.listAll(Auth.user(authentication));
  }

  /** The same board's gap numbers, so the board reads one list instead of one request per project. */
  @GetMapping("/api/gap")
  List<PostGapView> gapAll(Authentication authentication) {
    return posts.gapAll(Auth.user(authentication));
  }

  @GetMapping("/api/projects/{projectId}/gap")
  List<PostGapView> gap(@PathVariable long projectId, Authentication authentication) {
    return posts.gap(projectId, Auth.user(authentication));
  }

  @GetMapping("/api/posts/{id}")
  PostView get(@PathVariable long id, Authentication authentication) {
    return posts.get(id, Auth.user(authentication));
  }

  @PostMapping("/api/projects/{projectId}/posts")
  @ResponseStatus(HttpStatus.CREATED)
  PostView create(
      @PathVariable long projectId,
      @Valid @RequestBody PostCreate body,
      Authentication authentication) {
    return posts.create(projectId, body.draft(), Auth.user(authentication));
  }

  @PatchMapping("/api/posts/{id}")
  PostView update(
      @PathVariable long id,
      @Valid @RequestBody PostChange body,
      Authentication authentication) {
    return posts.update(id, body.draft(), Auth.user(authentication));
  }

  @PostMapping("/api/posts/{id}/cancel")
  PostView cancel(@PathVariable long id, Authentication authentication) {
    return posts.cancel(id, Auth.user(authentication));
  }

  record SkillBody(String skill, String kind, String band) {
    SkillDraft draft() {
      return new SkillDraft(skill, kind, band);
    }
  }

  record PostCreate(
      @Size(max = 120) String title,
      @NotBlank String role,
      @NotBlank String minJobLevel,
      @NotNull BigDecimal headcount,
      @NotNull LocalDate startDate,
      @NotNull LocalDate endDate,
      @NotBlank String travel,
      List<SkillBody> skills) {

    PostDraft draft() {
      return new PostDraft(
          title,
          role,
          minJobLevel,
          headcount,
          startDate,
          endDate,
          travel,
          skillDrafts(skills));
    }
  }

  /** Every field optional: an absent one keeps its stored value, an absent skill list keeps its rows. */
  record PostChange(
      String title,
      String role,
      String minJobLevel,
      BigDecimal headcount,
      LocalDate startDate,
      LocalDate endDate,
      String travel,
      List<SkillBody> skills) {

    PostDraft draft() {
      return new PostDraft(
          title,
          role,
          minJobLevel,
          headcount,
          startDate,
          endDate,
          travel,
          skillDrafts(skills));
    }
  }

  private static List<SkillDraft> skillDrafts(List<SkillBody> skills) {
    return skills == null ? null : skills.stream().map(SkillBody::draft).toList();
  }
}
