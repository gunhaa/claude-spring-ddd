package com.example.app.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

    private final TraceIdFilter filter = new TraceIdFilter();

    @Test
    @DisplayName("요청 헤더가 없으면 새 추적 ID를 만들어 MDC와 응답 헤더에 넣는다")
    void doFilter_withoutHeader_generatesTraceId() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> traceIdInChain = new AtomicReference<>();

        filter.doFilter(
                new MockHttpServletRequest(),
                response,
                (req, res) -> traceIdInChain.set(MDC.get(TraceIdFilter.MDC_KEY)));

        assertThat(traceIdInChain.get()).isNotBlank();
        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isEqualTo(traceIdInChain.get());
    }

    @Test
    @DisplayName("요청 헤더 값이 안전한 형식이면 그대로 이어 쓴다")
    void doFilter_withValidHeader_reusesIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, "client-req_01.a");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isEqualTo("client-req_01.a");
    }

    @Test
    @DisplayName("요청 헤더 값에 허용되지 않은 문자가 있으면 버리고 새로 만든다")
    void doFilter_withUnsafeHeader_replacesIt() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, "evil\nINFO forged log");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, (req, res) -> {});

        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isNotBlank().doesNotContain("evil");
    }

    @Test
    @DisplayName("허용 문자로만 된 64자 요청 헤더 값은 그대로 이어 쓴다")
    void doFilter_with64CharHeader_reusesIt() throws Exception {
        // given
        String headerValue = "a".repeat(64);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, headerValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> {});

        // then
        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isEqualTo(headerValue);
    }

    @Test
    @DisplayName("허용 문자로만 되어 있어도 65자인 요청 헤더 값은 버리고 새로 만든다")
    void doFilter_with65CharHeader_replacesIt() throws Exception {
        // given
        String headerValue = "a".repeat(65);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TraceIdFilter.HEADER_NAME, headerValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        filter.doFilter(request, response, (req, res) -> {});

        // then
        assertThat(response.getHeader(TraceIdFilter.HEADER_NAME)).isNotBlank().isNotEqualTo(headerValue);
    }

    @Test
    @DisplayName("요청 처리가 끝나면 MDC에서 추적 ID를 지운다")
    void doFilter_afterChain_clearsMdc() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {});

        assertThat(MDC.get(TraceIdFilter.MDC_KEY)).isNull();
    }
}
