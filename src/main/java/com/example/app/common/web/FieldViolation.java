package com.example.app.common.web;

/** 입력 검증 실패 한 건. 에러 응답의 {@code errors} 배열 원소로 나간다. */
public record FieldViolation(String field, String message) {}
