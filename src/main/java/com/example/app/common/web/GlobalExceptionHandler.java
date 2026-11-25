package com.example.app.common.web;

import com.example.app.common.error.BusinessException;
import com.example.app.common.error.CommonErrorCode;
import com.example.app.common.error.ErrorCode;
import com.example.app.common.error.ErrorType;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 모든 에러 응답을 RFC 9457 Problem Details 형식으로 통일한다.
 *
 * <pre>{@code
 * {
 *   "type": "about:blank",
 *   "title": "Not Found",
 *   "status": 404,
 *   "detail": "상품을 찾을 수 없습니다.",
 *   "instance": "/api/v1/products/1",
 *   "code": "PRODUCT_NOT_FOUND",
 *   "traceId": "3f2a...",
 *   "errors": [{"field": "name", "message": "..."}]   // 입력 검증 실패일 때만
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@link BusinessException}: {@link ErrorCode}의 {@link ErrorType}으로 상태 코드를 정한다.
 *   <li>입력 검증 실패: 400, {@code INVALID_INPUT}, {@code errors}에 필드별 사유.
 *   <li>Spring MVC 표준 예외(405, 415 등): 상태 코드는 Spring 기본값을 따르고 {@code code}·{@code traceId}만 덧붙인다.
 *   <li>그 밖의 예외: 500, {@code INTERNAL_ERROR}. 내부 메시지는 응답에 노출하지 않고 로그에만 남긴다.
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String TRACE_ID = "traceId";
    private static final String ERRORS = "errors";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        HttpStatus status = toHttpStatus(errorCode.type());
        if (status.is5xxServerError()) {
            log.error("{}: {}", errorCode.code(), ex.getMessage(), ex);
        } else {
            log.warn("{}: {}", errorCode.code(), ex.getMessage());
        }
        return problem(status, errorCode.code(), ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("처리되지 않은 예외", ex);
        CommonErrorCode errorCode = CommonErrorCode.INTERNAL_ERROR;
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, errorCode.code(), errorCode.message(), request);
    }

    /** {@code @Valid @RequestBody} 검증 실패. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toViolation)
                .toList();
        return handleExceptionInternal(ex, invalidInput(errors), headers, status, request);
    }

    /** {@code @RequestParam @Positive}처럼 컨트롤러 메서드 파라미터에 붙은 제약의 검증 실패. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldViolation> errors = ex.getParameterValidationResults().stream()
                .flatMap(result -> {
                    String field = result.getMethodParameter().getParameterName();
                    return result.getResolvableErrors().stream()
                            .map(error -> new FieldViolation(field, error.getDefaultMessage()));
                })
                .toList();
        return handleExceptionInternal(ex, invalidInput(errors), headers, status, request);
    }

    /** Spring MVC 표준 예외가 모두 거쳐 가는 지점. 응답 본문에 공통 필드를 덧붙인다. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        ResponseEntity<Object> response = super.handleExceptionInternal(ex, body, headers, statusCode, request);
        if (response != null && response.getBody() instanceof ProblemDetail problemDetail) {
            String code = codeOf(problemDetail, statusCode);
            if (statusCode.is5xxServerError()) {
                log.error("{}: {}", code, ex.getMessage(), ex);
            } else {
                // 4xx 표준 예외의 메시지에는 요청 값이 섞인다(검증 실패의 rejected value, JSON 파싱 오류의 원문 조각).
                // 개인정보가 로그에 남지 않도록 예외 종류만 남긴다. 필드별 사유는 응답의 errors에 있다.
                log.warn("{}: {}", code, ex.getClass().getSimpleName());
            }
            problemDetail.setProperty(CODE, code);
            problemDetail.setProperty(TRACE_ID, MDC.get(TraceIdFilter.MDC_KEY));
            if (problemDetail.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
                problemDetail.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
        }
        return response;
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String code, String detail, HttpServletRequest request) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setInstance(URI.create(request.getRequestURI()));
        problemDetail.setProperty(CODE, code);
        problemDetail.setProperty(TRACE_ID, MDC.get(TraceIdFilter.MDC_KEY));
        return ResponseEntity.status(status).body(problemDetail);
    }

    private static ProblemDetail invalidInput(List<FieldViolation> errors) {
        CommonErrorCode errorCode = CommonErrorCode.INVALID_INPUT;
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, errorCode.message());
        problemDetail.setProperty(CODE, errorCode.code());
        problemDetail.setProperty(ERRORS, errors);
        return problemDetail;
    }

    private static FieldViolation toViolation(FieldError error) {
        return new FieldViolation(error.getField(), error.getDefaultMessage());
    }

    /** 이미 정해진 code가 있으면 그대로 쓰고, 없으면 상태 코드로 정한다. */
    private static String codeOf(ProblemDetail problemDetail, HttpStatusCode statusCode) {
        if (problemDetail.getProperties() != null
                && problemDetail.getProperties().get(CODE) instanceof String code) {
            return code;
        }
        if (statusCode.is5xxServerError()) {
            return CommonErrorCode.INTERNAL_ERROR.code();
        }
        return switch (statusCode.value()) {
            case 400 -> CommonErrorCode.INVALID_INPUT.code();
            case 404 -> CommonErrorCode.RESOURCE_NOT_FOUND.code();
            default -> {
                // 405 → METHOD_NOT_ALLOWED, 415 → UNSUPPORTED_MEDIA_TYPE 처럼 HTTP 상태 이름을 쓴다
                HttpStatus status = HttpStatus.resolve(statusCode.value());
                yield status != null ? status.name() : String.valueOf(statusCode.value());
            }
        };
    }

    private static HttpStatus toHttpStatus(ErrorType type) {
        return switch (type) {
            case INVALID_INPUT -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
