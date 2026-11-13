package com.linzu.common.exception;

import com.linzu.common.api.Result;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusiness(BusinessException e) {
        return ResponseEntity.status(e.getErrorCode().getHttpStatus()).body(
            Result.fail(e.getCode(), e.getMessage())
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(
        MethodArgumentNotValidException e
    ) {
        return badRequest(firstError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        return badRequest(firstError(e.getBindingResult().getFieldError()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        return badRequest(
            e
                .getConstraintViolations()
                .stream()
                .findFirst()
                .map(v -> v.getMessage())
                .orElse(ErrorCode.PARAM_ERROR.getMessage())
        );
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
    })
    public ResponseEntity<Result<Void>> handleBadRequest(Exception e) {
        return badRequest(ErrorCode.PARAM_ERROR.getMessage());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Result<Void>> handleUploadTooLarge(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.FILE_TOO_LARGE.getHttpStatus()).body(
            Result.fail(ErrorCode.FILE_TOO_LARGE)
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotAllowed(
        HttpRequestMethodNotSupportedException e
    ) {
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(Result.fail(4050, "Method Not Allowed"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNoResource(NoResourceFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Result.fail(4040, "资源不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleOther(Exception e) {
        log.error("未处理异常", e);
        return ResponseEntity.status(ErrorCode.SYSTEM_ERROR.getHttpStatus()).body(
            Result.fail(ErrorCode.SYSTEM_ERROR)
        );
    }

    private ResponseEntity<Result<Void>> badRequest(String message) {
        return ResponseEntity.status(ErrorCode.PARAM_ERROR.getHttpStatus()).body(
            Result.fail(ErrorCode.PARAM_ERROR.getCode(), message)
        );
    }

    private String firstError(FieldError fieldError) {
        return fieldError == null
            ? ErrorCode.PARAM_ERROR.getMessage()
            : fieldError.getDefaultMessage();
    }
}
