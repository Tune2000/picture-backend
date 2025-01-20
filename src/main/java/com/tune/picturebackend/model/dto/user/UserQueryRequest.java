package com.tune.picturebackend.model.dto.user;

import com.tune.picturebackend.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.*;
import java.io.Serializable;

/**
 * @Author: Tune
 * @Description:用户查询（用户管理）
 */

@EqualsAndHashCode(callSuper = true)
@Data
public class UserQueryRequest extends PageRequest implements Serializable {
    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    @Size(min = 2, max = 16, message = "账号长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "账号只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userAccount;

    /**
     * 用户昵称
     */
    @Size(min = 2, max = 16, message = "昵称长度应在2到16个字符之间")
    @Pattern(regexp = "^[a-zA-Z0-9\u4e00-\u9fa5@#$%^&+=.]+$", message = "昵称只能包含大小写字母、数字、中文或特殊字符@#$%^&+=.")
    private String userName;

    /**
     * 用户角色：user/admin/ban
     */
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

    /**
     * 状态 0-活跃 1-锁定
     */
    @Min(value = 0, message = "状态必须在指定范围之间")
    @Max(value = 1, message = "状态必须在指定范围之间")
    private Integer userStatus;

    private static final long serialVersionUID = 1L;
}