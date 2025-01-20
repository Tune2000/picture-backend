package com.tune.picturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * @Author: Tune
 * @Description:用户登录请求体
 */
@Data
public class UserLoginRequest implements Serializable {
    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 用户账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 16, message = "账号长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "账号只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userAccount;

    /**
     * 用户密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 16, message = "密码长度应在8到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9@#$%^&+=.]+$", message = "密码只能包含大小写字母、数字和特殊字符@#$%^&+=.")
    private String userPassword;
}