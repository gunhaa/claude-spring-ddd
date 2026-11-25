package com.example.app.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.app.common.error.BusinessException;
import com.example.app.common.error.ErrorCode;
import com.example.app.common.error.ErrorType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
@ExtendWith(OutputCaptureExtension.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("BusinessException은 오류 코드의 종류에 맞는 상태 코드와 Problem Details로 응답한다")
    void businessException_respondsWithProblemDetail() throws Exception {
        mockMvc.perform(get("/test/business").header(TraceIdFilter.HEADER_NAME, "trace-1"))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conflict"))
                .andExpect(jsonPath("$.detail").value("샘플이 이미 있습니다."))
                .andExpect(jsonPath("$.instance").value("/test/business"))
                .andExpect(jsonPath("$.code").value("SAMPLE_DUPLICATED"))
                .andExpect(jsonPath("$.traceId").value("trace-1"));
    }

    @Test
    @DisplayName("요청 본문 검증에 실패하면 400, INVALID_INPUT과 필드별 사유로 응답한다")
    void invalidRequestBody_respondsWithFieldErrors() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.instance").value("/test/body"))
                .andExpect(jsonPath("$.errors[0].field").value("name"))
                .andExpect(jsonPath("$.errors[0].message").isNotEmpty())
                .andExpect(jsonPath("$.traceId").value(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("요청 본문 검증 실패 로그에는 클라이언트가 보낸 값이 남지 않는다")
    void invalidRequestBody_doesNotLogRejectedValue(CapturedOutput output) throws Exception {
        // given
        String personalValue = "hong-gildong-010-1234-5678";

        // when
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"홍길동\",\"email\":\"" + personalValue + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        // then
        assertThat(output.getOut()).contains("INVALID_INPUT").doesNotContain(personalValue);
    }

    @Test
    @DisplayName("요청 파라미터 검증에 실패하면 400, INVALID_INPUT과 파라미터별 사유로 응답한다")
    void invalidRequestParam_respondsWithFieldErrors() throws Exception {
        mockMvc.perform(get("/test/param").param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("size"));
    }

    @Test
    @DisplayName("Spring MVC 표준 예외는 기본 상태 코드를 유지하고 code와 traceId를 덧붙인다")
    void frameworkException_keepsStatusAndAddsCode() throws Exception {
        mockMvc.perform(delete("/test/param"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"))
                .andExpect(jsonPath("$.instance").value("/test/param"))
                .andExpect(jsonPath("$.traceId").value(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("처리되지 않은 예외는 500, INTERNAL_ERROR로 응답하고 내부 메시지를 노출하지 않는다")
    void unexpectedException_hidesInternalMessage() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("서버 내부 오류가 발생했습니다."))
                .andExpect(header().exists(TraceIdFilter.HEADER_NAME));
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ErrorType.class)
    @DisplayName("BusinessException은 ErrorType마다 정해진 상태 코드와 ErrorCode의 code·메시지로 응답한다")
    void businessException_eachErrorType_respondsWithMappedStatus(ErrorType type) throws Exception {
        // given
        TypedErrorCode errorCode = new TypedErrorCode(type);
        int expectedStatus = expectedStatus(type).value();

        // when / then
        mockMvc.perform(get("/test/business/{type}", type))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andExpect(jsonPath("$.code").value(errorCode.code()))
                .andExpect(jsonPath("$.detail").value(errorCode.message()));
    }

    @Test
    @DisplayName("매핑되지 않은 경로를 요청하면 404, RESOURCE_NOT_FOUND로 응답한다")
    void get_unmappedPath_respondsWithResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/no-such-path"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"))
                .andExpect(jsonPath("$.instance").value("/test/no-such-path"))
                .andExpect(jsonPath("$.traceId").value(not(emptyOrNullString())));
    }

    @Test
    @DisplayName("JSON 본문이 깨져 읽을 수 없으면 400, INVALID_INPUT으로 응답한다")
    void post_malformedJsonBody_respondsWithInvalidInput() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.instance").value("/test/body"));
    }

    @Test
    @DisplayName("BusinessException 에러 응답에도 X-Request-Id 헤더가 있고 본문 traceId와 같다")
    void businessException_respondsWithRequestIdHeaderEqualToTraceId() throws Exception {
        // given / when
        MvcResult result = mockMvc.perform(get("/test/business")).andReturn();

        // then
        assertRequestIdHeaderEqualsTraceId(result);
    }

    @Test
    @DisplayName("처리되지 않은 예외의 500 응답에도 X-Request-Id 헤더가 있고 본문 traceId와 같다")
    void unexpectedException_respondsWithRequestIdHeaderEqualToTraceId() throws Exception {
        // given / when
        MvcResult result = mockMvc.perform(get("/test/unexpected")).andReturn();

        // then
        assertThat(result.getResponse().getStatus()).isEqualTo(500);
        assertRequestIdHeaderEqualsTraceId(result);
    }

    private static void assertRequestIdHeaderEqualsTraceId(MvcResult result) throws Exception {
        String requestId = result.getResponse().getHeader(TraceIdFilter.HEADER_NAME);
        assertThat(requestId).isNotBlank();
        jsonPath("$.traceId").value(requestId).match(result);
    }

    private static HttpStatus expectedStatus(ErrorType type) {
        return switch (type) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    enum TestErrorCode implements ErrorCode {
        SAMPLE_DUPLICATED(ErrorType.CONFLICT, "샘플이 이미 있습니다.");

        private final ErrorType type;
        private final String message;

        TestErrorCode(ErrorType type, String message) {
            this.type = type;
            this.message = message;
        }

        @Override
        public String code() {
            return name();
        }

        @Override
        public ErrorType type() {
            return type;
        }

        @Override
        public String message() {
            return message;
        }
    }

    /** ErrorType마다 하나씩 만드는 테스트용 오류 코드. enum에 새 값이 생겨도 따로 추가할 필요가 없다. */
    record TypedErrorCode(ErrorType type) implements ErrorCode {

        @Override
        public String code() {
            return "SAMPLE_" + type.name();
        }

        @Override
        public String message() {
            return type.name() + " 샘플 오류입니다.";
        }
    }

    record SampleRequest(@NotBlank String name, @Email String email) {}

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(TestErrorCode.SAMPLE_DUPLICATED);
        }

        @GetMapping("/test/business/{type}")
        void businessOfType(@PathVariable ErrorType type) {
            throw new BusinessException(new TypedErrorCode(type));
        }

        @PostMapping("/test/body")
        void body(@Valid @RequestBody SampleRequest request) {}

        @GetMapping("/test/param")
        void param(@RequestParam @Positive int size) {}

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("DB 비밀번호가 틀렸습니다");
        }
    }
}
