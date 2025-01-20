package com.tune.picturebackend.exception;

import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import javax.validation.ConstraintViolation;

import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * @Author: Tune
 * @Description:
 * @CreateTime: 2025-01-10
 */

@RestControllerAdvice
@Slf4j
@Order
public class ValidExceptionHandler {
    /**
     * 处理请求参数格式错误 @RequestBody上使用@Valid 实体上使用@NotNull等，验证失败后抛出的异常是MethodArgumentNotValidException异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public BaseResponse<?> methodArgumentNotValidExceptionHandler(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("，"));
        log.error("参数校验异常: {}", message);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }


    /**
     * 处理Get请求中 使用@Valid 验证路径中请求实体校验失败后抛出的异常
     */
    @ExceptionHandler(BindException.class)
    @ResponseBody
    public BaseResponse<?> BindExceptionHandler(BindException e) {
        String message = e.getBindingResult().getAllErrors().stream().map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 处理请求参数格式错误 @RequestParam上validate失败后抛出的异常是ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    public BaseResponse<?> ConstraintViolationExceptionHandler(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream().map(ConstraintViolation::getMessage).collect(Collectors.joining());
        return ResultUtils.error(ErrorCode.PARAMS_ERROR, message);
    }

    /**
     * 参数格式异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseBody
    public BaseResponse<?> HttpMessageNotReadableExceptionHandler(HttpMessageNotReadableException e) {
        log.error("请求参数格式错误: {}", e.getMessage(), e);
        return ResultUtils.error(ErrorCode.PARAMS_ERROR);
    }

}
