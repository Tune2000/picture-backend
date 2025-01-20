package com.tune.picturebackend.model.vo.user;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Tune
 * @Description:
 */
@Data
public class LoginUserInfoVO implements Serializable {
    /**
      * 用户 id
      */
     private Long id;

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
      * 性别 0-待定 1-男 2-女
      */
     private Integer gender;

     /**
      * 电话
      */
     private String phone;

     /**
      * 邮箱
      */
     private String email;

}
