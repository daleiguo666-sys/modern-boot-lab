package com.mega.lab.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 全局异常处理 —— 统一产出 RFC 9457（原 RFC 7807）的 ProblemDetail。
 *
 * 和你过去 `Results.fail(code, msg)` 的差异与好处：
 *  - ProblemDetail 是 Spring 6 内置类型，响应 Content-Type 自动是 application/problem+json（业界标准）。
 *  - 固定字段：type / title / status / detail / instance，再加自定义扩展字段（这里加了 errors、timestamp）。
 *  - 不用每个项目自己发明一套 {code, message, data} 结构；网关、前端、APM 都能按标准解析。
 *
 * 注意：成功响应你仍可保留自己的包装（或直接返回 DTO）；ProblemDetail 专门管“错误”这一侧。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "资源不存在", ex.getMessage(), "resource-not-found");
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, "资源冲突", ex.getMessage(), "duplicate-resource");
    }

    /** Bean Validation（@Valid）失败：聚合所有字段错误，返回 400。 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        ProblemDetail pd = build(HttpStatus.BAD_REQUEST, "参数校验失败", "请求体存在非法字段", "validation-error");
        List<String> errors = new ArrayList<>();
        for (var fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(fieldError.getField() + ": " + fieldError.getDefaultMessage());
        }
        pd.setProperty("errors", errors);
        return pd;
    }

    /** 兜底：未预料的异常返回 500，但不要把堆栈泄露给客户端。 */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex, WebRequest request) {
        // 真实项目这里会 log.error(...) 并接入 Sentry/告警；detail 给个通用文案即可。
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误", "请稍后重试", "internal-error");
    }

    private ProblemDetail build(HttpStatus status, String title, String detail, String typeSlug) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        pd.setType(URI.create("https://errors.mega.lab/" + typeSlug));
        pd.setProperty("timestamp", Instant.now());
        return pd;
    }
}
