package com.example.staffing.api;

import com.example.staffing.domain.ReleaseService;
import com.example.staffing.domain.ReleaseView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** Releasing a confirmed person. The service decides who may, so the screen is not the authority. */
@RestController
public class AssignmentController {

  private final ReleaseService releases;

  public AssignmentController(ReleaseService releases) {
    this.releases = releases;
  }

  @PostMapping("/api/assignments/{id}/release")
  ReleaseView release(@PathVariable long id, Authentication authentication) {
    return releases.release(id, Auth.user(authentication));
  }
}
