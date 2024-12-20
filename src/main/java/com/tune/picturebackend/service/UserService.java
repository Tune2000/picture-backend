package com.tune.picturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.tune.picturebackend.model.entity.User;
import com.tune.picturebackend.model.vo.LoginUserVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author Tune
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2024-12-20 15:41:56
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return 用户id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 用户脱敏信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);


    /**
     * 获取加密之后的密码
     * @param userPassword
     * @return 加密之后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的用户信息
     * @param user
     * @return 用户脱敏信息
     */
    LoginUserVO getLoginUserVO(User user);
}
