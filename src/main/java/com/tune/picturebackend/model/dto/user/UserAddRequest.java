package com.tune.picturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * @Author: Tune
 * @Description: 用户新增（用户管理）
 */
@Data
public class UserAddRequest implements Serializable {
    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 16, message = "账号长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "账号只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 16, message = "密码长度应在8到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9@#$%^&+=.]+$", message = "密码只能包含大小写字母、数字和特殊字符@#$%^&+=.")
    private String userPassword;

    /**
     * 用户昵称
     */
    @Size(min = 2, max = 16, message = "昵称长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "昵称只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userName;

    /**
     * 用户简介
     */
    @Size(max = 256, message = "用户简介长度不能超过256个字符")
    private String userProfile;

    /**
     * 用户角色：user/admin/root
     */
    @NotBlank(message = "用户角色不能为空")
    @Pattern(regexp = "^(user|admin|root)$", message = "用户角色必须是指定角色之一")
    private String userRole;

    /**
     * 性别 0-待定 1-男 2-女
     */
    @Min(value = 0, message = "性别必须在指定范围之间")
    @Max(value = 2, message = "性别必须在指定范围之间")
    private Integer gender;

    /**
     * 电话
     */
    @Pattern(regexp = "^\\d{11}$", message = "电话号码格式不正确，应为11位数字")
    private String phone;

    /**
     * 邮箱
     */
    @Email(message = "邮箱格式不正确")
    private String email;


    private static final long serialVersionUID = 1L;
}