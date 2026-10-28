package com.kisandaily.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kisandaily.response.ApiError;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.MediaType;

public final class HttpErrorWriter {
  private HttpErrorWriter() {}

  public static void write(
      ObjectMapper json, HttpServletResponse response, int status, String code, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    json.writeValue(response.getWriter(), new ApiError(code, message, Map.of()));
  }
}
