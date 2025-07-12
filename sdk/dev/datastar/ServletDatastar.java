package dev.datastar;

import java.io.IOException;
import java.util.stream.Collectors;

import dev.datastar.Datastar.Event;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ServletDatastar {
  public static void prepareResponse(HttpServletResponse response) {
    response.setContentType("text/event-stream");
    response.addHeader("Connection", "keep-alive");
    response.addHeader("Cache-Control", "no-cache");
  }

  public static void send(HttpServletResponse response, Event event) throws IOException {
    response.getWriter().append(build(event));
    response.getWriter().flush();
  }

  private static String build(Event event) {
    var sseEvent = new StringBuilder();
    sseEvent.append("event:").append(event.name()).append('\n');
    if (event.id() != null)
      sseEvent.append("id:").append(event.id()).append('\n');
    if (event.reconnectDelay() != null)
      sseEvent.append("retry:").append(event.reconnectDelay()).append('\n');
    sseEvent.append(event.data().lines().map(data -> "data:" + data).collect(Collectors.joining("\n"))).append("\n\n");
    return sseEvent.toString();
  }

  public static class DatastarHeaderFilter implements Filter {
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
        throws IOException, ServletException {
      var httpRequest = (HttpServletRequest) request;
      if ("true".equals(httpRequest.getHeader("datastar-request")))
        prepareResponse((HttpServletResponse) response);
      chain.doFilter(request, response);
    }
  }
}
