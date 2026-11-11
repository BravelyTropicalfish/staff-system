package com.example.staffing.api;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 每个标签页自己的身份。Servlet 会话是一个浏览器一个 cookie，两个标签页就只能是同一个账号；所以
 * 登录时除了写会话，还发一个只属于该标签页的令牌，页面试图用它压过 cookie。令牌只存在本进程内存
 * 里：服务重启后全部失效，页面重新登录即可。
 */
@Component
public class SessionTokens {

  /** 页面在每个请求上带这个头；没有它就退回到 cookie，API 客户端与测试仍按老办法工作。 */
  static final String HEADER = "X-Session-Token";

  private final Map<String, SecurityContext> tokens = new ConcurrentHashMap<>();

  /** 存一份副本，请求里对 holder 的改动不会反过来改写已发的令牌。 */
  String issue(SecurityContext context) {
    SecurityContext copy = SecurityContextHolder.createEmptyContext();
    copy.setAuthentication(context.getAuthentication());
    String token = UUID.randomUUID().toString();
    tokens.put(token, copy);
    return token;
  }

  Optional<SecurityContext> find(String token) {
    return token == null ? Optional.empty() : Optional.ofNullable(tokens.get(token));
  }

  void revoke(String token) {
    if (token != null) {
      tokens.remove(token);
    }
  }
}
