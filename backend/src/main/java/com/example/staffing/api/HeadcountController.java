package com.example.staffing.api;

import com.example.staffing.domain.HeadcountService;
import com.example.staffing.domain.HeadcountView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The headcount report's only route: one read, the coordinator's alone. It takes no query parameter,
 * so no filter can narrow the pool, and it answers no write verb because no handler exists — the same
 * shape {@code /api/people} already has.
 */
@RestController
@RequestMapping("/api/headcount")
public class HeadcountController {

  private final HeadcountService headcount;

  public HeadcountController(HeadcountService headcount) {
    this.headcount = headcount;
  }

  @GetMapping
  public HeadcountView report(Authentication authentication) {
    Auth.coordinator(authentication);
    return headcount.report();
  }
}
