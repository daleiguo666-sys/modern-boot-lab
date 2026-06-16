package com.mega.lab.common;

/**
 * 业务异常 —— 由 GlobalExceptionHandler 翻译成 RFC 9457 ProblemDetail。
 * 不要在 Service 里直接 new ResponseEntity 或拼 Result.fail()，让异常向上抛、由统一处理器收口。
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
