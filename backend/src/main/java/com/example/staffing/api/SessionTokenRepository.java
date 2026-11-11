package com.example.staffing.api;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

/**
 * 浏览器用 cookie，标签页用令牌。带令牌的请求只按令牌回答：令牌不认识就当未登录，绝不回落到
 * cookie —— 否则一个过期的标签页会静默变成另一个标签页的账号。不带令牌的请求保持原样，仍由会话
 * cookie 决定身份，API 客户端与测试套件因此不受影响。
 */
public final class SessionTokenRepository implements SecurityContextRepository {

  private final HttpSessionSecurityContextRepository sessions =
      new HttpSessionSecurityContextRepository();
  private final SessionTokens tokens;

  public SessionTokenRepository(SessionTokens tokens) {
    this.tokens = tokens;
  }

  @Override
  public SecurityContext loadContext(HttpRequestResponseHolder holder) {
    String token = holder.getRequest().getHeader(SessionTokens.HEADER);
    if (token != null) {
      return tokens.find(token).orElseGet(SecurityContextHolder::createEmptyContext);
    }
    return sessions.loadContext(holder);
  }

  @Override
  public void saveContext(
      SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    // A token request keeps its identity in the token; writing the session too would tie the tab
    // back to the cookie every other tab shares.
    if (request.getHeader(SessionTokens.HEADER) == null) {
      sessions.saveContext(context, request, response);
    }
  }

  @Override
  public boolean containsContext(HttpServletRequest request) {
    String token = request.getHeader(SessionTokens.HEADER);
    return token != null ? tokens.find(token).isPresent() : sessions.containsContext(request);
  }
}
