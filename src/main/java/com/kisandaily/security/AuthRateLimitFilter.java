package com.kisandaily.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisandaily.util.HttpErrorWriter;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthRateLimitFilter extends OncePerRequestFilter {
  private final ObjectMapper json;
  private final int limit;
  private final Map<String, Bucket> buckets = new HashMap<>();

  private record Bucket(long minute, int count) {}

  public AuthRateLimitFilter(ObjectMapper json, int limit) {
    this.json = json;
    this.limit = Math.max(1, limit);
  }

  private synchronized boolean allowed(String ip) {
    long minute = System.currentTimeMillis() / 60000;
    buckets.entrySet().removeIf(e -> e.getValue().minute() != minute);
    Bucket bucket = buckets.get(ip);
    if (bucket == null && buckets.size() >= 10000) return false;
    if (bucket != null && bucket.count() >= limit) return false;
    buckets.put(ip, new Bucket(minute, bucket == null ? 1 : bucket.count() + 1));
    return true;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    if (request.getMethod().equals("POST")
        && Set.of("/api/auth/login", "/api/auth/register", "/api/auth/refresh")
            .contains(request.getRequestURI())
        && !allowed(request.getRemoteAddr())) {
      response.setHeader("Retry-After", "60");
      HttpErrorWriter.write(
          json,
          response,
          429,
          "RATE_LIMITED",
          "बहुत कोशिशें हो गई हैं। एक मिनट बाद फिर कोशिश करें");
      return;
    }
    chain.doFilter(request, response);
  }
}
