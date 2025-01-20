package com.tune.picturebackend.exception;

import cn.dev33.satoken.exception.NotLoginException;
import cn.dev33.satoken.exception.NotRoleException;
import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.ResultUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Author: Tune
 * @Description:sa-token异常处理
 */
@RestControllerAdvice
@Slf4j
@Order(1)
public class SaTokenExceptionHandler {
    // sa-token 登录校验异常捕获
    @ExceptionHandler(NotLoginException.class)
    public BaseResponse<?> handlerNotLoginException(NotLoginException nle) {
        // 不同异常返回不同状态码
        String message;
        if (nle.getType().equals(NotLoginException.NOT_TOKEN)) {
            message = "未提供Token";
        } else if (nle.getType().equals(NotLoginException.INVALID_TOKEN)) {
            message = "未提供有效的Token";
        } else if (nle.getType().equals(NotLoginException.TOKEN_TIMEOUT)) {
            message = "登录信息已过期，请重新登录";
        } else if (nle.getType().equals(NotLoginException.BE_REPLACED)) {
            message = "您的账户已在另一台设备上登录，如非本人操作，请立即修改密码";
        } else if (nle.getType().equals(NotLoginException.KICK_OUT)) {
            message = "已被系统强制下线";
        } else {
            message = "当前会话未登录";
        }
        log.info(message,nle);
        return ResultUtils.error(ErrorCode.NOT_LOGIN_ERROR, message);
    }

    // sa-token 角色校验异常捕获
    @ExceptionHandler
    public BaseResponse<?> handlerNotRoleException(NotRoleException e) {
        log.info("无角色权限",e);
        return ResultUtils.error(ErrorCode.NO_AUTH_ERROR, "无此权限：" + e.getCode());
    }
}
