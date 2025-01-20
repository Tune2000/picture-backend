package com.tune.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.model.dto.user.UserAddRequest;
import com.tune.picturebackend.model.dto.user.UserQueryRequest;
import com.tune.picturebackend.model.dto.user.UserUpdateByLoginUserRequest;
import com.tune.picturebackend.model.dto.user.UserUpdateRequest;
import com.tune.picturebackend.model.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tune.picturebackend.model.vo.user.LoginUserInfoVO;
import com.tune.picturebackend.model.vo.user.LoginUserVO;
import com.tune.picturebackend.model.vo.user.UserVO;

import java.util.List;

/**
* @author Tune
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2025-01-03 14:02:03
*/
public interface UserService extends IService<User> {
    /**
     * 用户注册
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @param checkPassword 确认密码
     * @return
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户登录
     * @param userAccount 用户账户
     * @param userPassword 用户密码
     * @return 用户脱敏信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword);

    /**
     * 获取当前登录用户
     *
     * @return
     */
    User getLoginUser();

    /**
     * 获取加密之后的密码
     * @param userPassword
     * @return 加密之后的密码
     */
    String getEncryptPassword(String userPassword);

    /**
     * 完整已登录用户信息脱敏
     * @param user
     * @return 已登录用户脱敏信息
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取单个用户脱敏信息(管理员)
     * @param user
     * @return 用户脱敏信息
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户信息（个人信息）
     * @param user
     * @return 用户信息
     */
    LoginUserInfoVO getLoginUserInfoVO(User user);


    /**
     * 更新用户信息（个人信息）
     * @param userUpdateByLoginUserRequest
     * @return 是否更新成功
     */
    boolean updateLoginUserInfo(UserUpdateByLoginUserRequest userUpdateByLoginUserRequest);

    /**
     * 添加用户(管理员)
     * @param userAddRequest
     * @return 用户id
     */
    Long addUser(UserAddRequest userAddRequest);

    /**
     * 更新用户信息(管理员)
     * @param userUpdateRequest
     * @return 是否更新成功
     */
    Boolean updateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 删除用户(管理员)
     * @param deleteRequest
     * @return 是否删除成功
     */
    Boolean deleteUser(DeleteRequest deleteRequest);

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return 用户脱敏信息列表
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return 查询条件
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);


}
