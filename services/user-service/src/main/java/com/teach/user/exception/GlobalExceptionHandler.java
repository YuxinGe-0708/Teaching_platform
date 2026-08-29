package com.teach.user.exception;

import com.teach.user.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<String> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldError() != null
                ? ex.getBindingResult().getFieldError().getDefaultMessage()
                : "参数校验失败";
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), msg);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ApiResponse.fail(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ApiResponse<String> handleIllegalState(IllegalStateException ex) {
        return ApiResponse.fail(HttpStatus.FORBIDDEN.value(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<String> handleGeneric(Exception ex) {
        return ApiResponse.fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "服务器内部错误");
    }
}
