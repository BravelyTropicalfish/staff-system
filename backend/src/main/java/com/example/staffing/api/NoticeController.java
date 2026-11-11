package com.example.staffing.api;

import com.example.staffing.domain.NoticeService;
import com.example.staffing.domain.NoticeView;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** The coordinator's notice list, read and dismissed. A lead is refused both routes. */
@RestController
public class NoticeController {

  private final NoticeService notices;

  public NoticeController(NoticeService notices) {
    this.notices = notices;
  }

  @GetMapping("/api/notices")
  List<NoticeView> list(Authentication authentication) {
    return notices.list(Auth.user(authentication));
  }

  @PostMapping("/api/notices/{id}/dismiss")
  NoticeView dismiss(@PathVariable long id, Authentication authentication) {
    return notices.dismiss(id, Auth.user(authentication));
  }
}
