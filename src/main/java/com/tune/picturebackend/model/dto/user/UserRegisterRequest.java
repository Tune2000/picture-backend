package com.tune.picturebackend.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Tune
 * @Description:用户注册请求体
 * @CreateTime: 2024-12-20
 */

@Data
public class UserRegisterRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;
    /*
     * 用户账号
     */
    private String userAccount;

    /*
     * 用户密码
     */
    private String userPassword;

    /*
     * 校验密码
     */
    private String checkPassword;
}
