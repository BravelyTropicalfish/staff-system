package com.example.staffing.api;

import com.example.staffing.domain.AllocationException;
import com.example.staffing.domain.CatalogException;
import com.example.staffing.domain.NoticeException;
import com.example.staffing.domain.PostException;
import com.example.staffing.domain.ProjectException;
import com.example.staffing.domain.RecommendationException;
import com.example.staffing.domain.UnreadableWorkbookException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Keeps the one error shape of the API: code, message, details — never a stack trace or a path. */
@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(BadCredentialsException.class)
  ResponseEntity<Map<String, Object>> wrongCredentials() {
    return error(HttpStatus.UNAUTHORIZED, "invalid credentials", "账号或密码不正确。");
  }

  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<Map<String, Object>> notYours() {
    return error(HttpStatus.FORBIDDEN, "forbidden", "这个动作不属于你。");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String, Object>> invalidBody(MethodArgumentNotValidException refused) {
    List<String> details =
        refused.getBindingResult().getFieldErrors().stream()
            .map(field -> field.getField() + ": " + field.getDefaultMessage())
            .toList();
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(
            Map.of(
                "code", "invalid",
                "message", "请求内容不合法。",
                "details", details));
  }

  /** A body that does not parse, or carries a field no request record declares. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<Map<String, Object>> unreadableBody() {
    return error(HttpStatus.BAD_REQUEST, "unreadable body", "请求体无法解析。");
  }

  @ExceptionHandler(CatalogException.class)
  ResponseEntity<Map<String, Object>> catalogRule(CatalogException refused) {
    return error(statusOf(refused.code()), refused.code(), refused.getMessage());
  }

  @ExceptionHandler(AllocationException.class)
  ResponseEntity<Map<String, Object>> allocationRule(AllocationException refused) {
    HttpStatus status =
        "invalid allocation".equals(refused.code())
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.CONFLICT;
    return error(status, refused.code(), refused.getMessage());
  }

  @ExceptionHandler(PostException.class)
  ResponseEntity<Map<String, Object>> postRule(PostException refused) {
    HttpStatus status =
        "invalid post".equals(refused.code())
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.CONFLICT;
    return error(status, refused.code(), refused.getMessage());
  }

  @ExceptionHandler(ProjectException.class)
  ResponseEntity<Map<String, Object>> projectRule(ProjectException refused) {
    HttpStatus status =
        "invalid project".equals(refused.code())
            ? HttpStatus.UNPROCESSABLE_ENTITY
            : HttpStatus.CONFLICT;
    return error(status, refused.code(), refused.getMessage());
  }

  /** Every refusal of a recommendation is a state conflict: the post, the person or the gate moved. */
  @ExceptionHandler(RecommendationException.class)
  ResponseEntity<Map<String, Object>> recommendationRule(RecommendationException refused) {
    return error(HttpStatus.CONFLICT, refused.code(), refused.getMessage());
  }

  @ExceptionHandler(NoticeException.class)
  ResponseEntity<Map<String, Object>> noticeRule(NoticeException refused) {
    return error(HttpStatus.CONFLICT, refused.code(), refused.getMessage());
  }

  @ExceptionHandler(UnreadableWorkbookException.class)
  ResponseEntity<Map<String, Object>> unreadableWorkbook(UnreadableWorkbookException refused) {
    return error(HttpStatus.UNPROCESSABLE_ENTITY, "unreadable workbook", refused.getMessage());
  }

  @ExceptionHandler(NoSuchElementException.class)
  ResponseEntity<Map<String, Object>> missingRecord(NoSuchElementException refused) {
    return error(HttpStatus.NOT_FOUND, "not found", refused.getMessage());
  }

  private static HttpStatus statusOf(String code) {
    return switch (code) {
      case "not found" -> HttpStatus.NOT_FOUND;
      case "invalid name", "invalid role domain" -> HttpStatus.UNPROCESSABLE_ENTITY;
      default -> HttpStatus.CONFLICT;
    };
  }

  private static ResponseEntity<Map<String, Object>> error(
      HttpStatus status, String code, String message) {
    return ResponseEntity.status(status)
        .body(Map.of("code", code, "message", message, "details", List.of()));
  }
}
