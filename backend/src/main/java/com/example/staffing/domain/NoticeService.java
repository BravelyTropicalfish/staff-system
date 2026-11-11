package com.example.staffing.domain;

import com.example.staffing.data.NoticeRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The coordinator's notice list. Only the coordinator reads it and only the coordinator dismisses
 * an entry, and a dismissal records the time; the entry leaves the list then and never before.
 */
@Service
public class NoticeService {

  static final String ALREADY_DISMISSED = "already dismissed";

  private final NoticeRepository notices;

  public NoticeService(NoticeRepository notices) {
    this.notices = notices;
  }

  @Transactional(readOnly = true)
  public List<NoticeView> list(AuthenticatedUser actor) {
    requireCoordinator(actor);
    return notices.listOpen(actor.id()).stream().map(NoticeService::view).toList();
  }

  @Transactional
  public NoticeView dismiss(long id, AuthenticatedUser actor) {
    requireCoordinator(actor);
    NoticeRepository.Row row = find(id);
    if (row.coordinatorId() != actor.id()) {
      throw new AccessDeniedException("this notice is not in your list");
    }
    if (row.dismissedAt() != null) {
      throw new NoticeException(ALREADY_DISMISSED, "the notice is dismissed already");
    }
    notices.dismiss(id, actor.id());
    return view(find(id));
  }

  private static void requireCoordinator(AuthenticatedUser actor) {
    if (!actor.isCoordinator()) {
      throw new AccessDeniedException("the notice list belongs to the coordinator");
    }
  }

  private NoticeRepository.Row find(long id) {
    return notices.findById(id).orElseThrow(() -> new NoSuchElementException("No notice " + id));
  }

  private static NoticeView view(NoticeRepository.Row row) {
    return new NoticeView(
        row.id(),
        row.postId(),
        row.postTitle(),
        row.personNo(),
        row.personName(),
        row.reason(),
        row.createdAt().toString(),
        row.dismissedAt() == null ? null : row.dismissedAt().toString());
  }
}
