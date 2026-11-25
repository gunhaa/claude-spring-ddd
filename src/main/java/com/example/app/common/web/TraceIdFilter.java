package com.example.app.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 추적 ID를 정해 MDC와 응답 헤더에 넣는다. 로그(ECS)와 에러 응답(ProblemDetail의 {@code traceId})이 같은 값을
 * 쓰므로, 클라이언트가 받은 응답으로 서버 로그를 찾을 수 있다.
 *
 * <p>요청에 {@code X-Request-Id} 헤더가 있고 형식이 안전하면 그 값을 이어 쓰고, 없으면 새로 만든다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Request-Id";
    /** ECS 표준 필드 trace.id. ECS 로그에서 {"trace":{"id":...}}로 출력된다. */
    public static final String MDC_KEY = "trace.id";

    /** 로그 인젝션을 막기 위해 외부에서 받은 값은 이 형식일 때만 쓴다. */
    private static final Pattern ALLOWED = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String traceId = resolve(request.getHeader(HEADER_NAME));
        MDC.put(MDC_KEY, traceId);
        response.setHeader(HEADER_NAME, traceId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    private static String resolve(String headerValue) {
        if (headerValue != null && ALLOWED.matcher(headerValue).matches()) {
            return headerValue;
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
