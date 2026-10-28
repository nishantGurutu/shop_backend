package com.kisandaily;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisandaily.security.AuthRateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.*;

class AuthThrottleTest {
  @Test
  void repeatedAuthRequestsAreThrottled() throws Exception {
    var filter = new AuthRateLimitFilter(new ObjectMapper(), 2);
    for (int i = 0; i < 3; i++) {
      var request = new MockHttpServletRequest("POST", "/api/auth/login");
      request.setRemoteAddr("192.0.2.1");
      var response = new MockHttpServletResponse();
      filter.doFilter(request, response, new MockFilterChain());
      assertThat(response.getStatus()).isEqualTo(i < 2 ? 200 : 429);
      if (i == 2) assertThat(response.getHeader("Retry-After")).isEqualTo("60");
    }
  }
}
