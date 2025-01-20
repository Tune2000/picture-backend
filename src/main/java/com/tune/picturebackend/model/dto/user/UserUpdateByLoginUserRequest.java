package com.tune.picturebackend.model.dto.user;

import lombok.Data;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * @Author: Tune
 * @Description: 修改个人信息
 */

@Data
public class UserUpdateByLoginUserRequest implements Serializable {
    /**
     * id
     */
    @NotNull(message = "id不能为空")
    @Positive(message = "id必须为正数")
    private Long id;
    /**
     * 用户昵称
     */
    @Size(min = 2, max = 16, message = "昵称长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "昵称只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    @Size(max = 256, message = "用户简介长度不能超过256个字符")
    private String userProfile;

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