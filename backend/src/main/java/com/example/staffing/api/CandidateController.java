package com.example.staffing.api;

import com.example.staffing.domain.CandidateSearchView;
import com.example.staffing.domain.MatchingService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The candidate search. It is the coordinator's alone, for any post: a lead sees only the
 * recommendations made to their own posts, so the full suitable list and the exclusion list are
 * never reachable with a lead account — not even the lead who owns the post.
 */
@RestController
public class CandidateController {

  private final MatchingService matching;

  public CandidateController(MatchingService matching) {
    this.matching = matching;
  }

  @GetMapping("/api/posts/{id}/candidates")
  CandidateSearchView search(@PathVariable long id, Authentication authentication) {
    Auth.coordinator(authentication);
    return matching.search(id);
  }
}
