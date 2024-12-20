package com.tune.picturebackend.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * @Author: Tune
 * @Description:已登录用户信息脱敏
 * @CreateTime: 2024-12-20
 */

@Data
public class LoginUserVO {
    /**
     * 用户 id
     */
    private Long id;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 用户昵称
     */
    private String userName;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
