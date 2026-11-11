package com.example.staffing.api;

import com.example.staffing.domain.ConfirmationService;
import com.example.staffing.domain.ConfirmationView;
import com.example.staffing.domain.PostRecommendationView;
import com.example.staffing.domain.RecommendationService;
import com.example.staffing.domain.RecommendationView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * The proposal and the three decisions that close it. The coordinator proposes and withdraws their
 * own proposal and never confirms; the lead of the post's project confirms or rejects, and nobody
 * else does — every verb hands the signed-in account to the service, which is where scope is judged.
 */
@RestController
public class RecommendationController {

  private final RecommendationService recommendations;
  private final ConfirmationService confirmations;

  public RecommendationController(
      RecommendationService recommendations, ConfirmationService confirmations) {
    this.recommendations = recommendations;
    this.confirmations = confirmations;
  }

  @GetMapping("/api/posts/{id}/recommendations")
  List<PostRecommendationView> list(@PathVariable long id, Authentication authentication) {
    return recommendations.list(id, Auth.user(authentication));
  }

  @PostMapping("/api/posts/{id}/recommendations")
  @ResponseStatus(HttpStatus.CREATED)
  RecommendationView recommend(
      @PathVariable long id,
      @Valid @RequestBody RecommendCandidate body,
      Authentication authentication) {
    return recommendations.recommend(id, body.personNo(), Auth.coordinator(authentication));
  }

  @PostMapping("/api/recommendations/{id}/confirm")
  ConfirmationView confirm(@PathVariable long id, Authentication authentication) {
    return confirmations.confirm(id, Auth.user(authentication));
  }

  @PostMapping("/api/recommendations/{id}/reject")
  RecommendationView reject(
      @PathVariable long id,
      @Valid @RequestBody(required = false) RejectProposal body,
      Authentication authentication) {
    return confirmations.reject(id, body == null ? null : body.note(), Auth.user(authentication));
  }

  @DeleteMapping("/api/recommendations/{id}")
  RecommendationView withdraw(@PathVariable long id, Authentication authentication) {
    return confirmations.withdraw(id, Auth.user(authentication));
  }

  record RecommendCandidate(@NotBlank String personNo) {}

  record RejectProposal(@Size(max = 500) String note) {}
}
