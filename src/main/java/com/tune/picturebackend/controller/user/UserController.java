package com.tune.picturebackend.controller.user;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaIgnore;
import cn.dev33.satoken.annotation.SaMode;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.common.ResultUtils;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.model.dto.user.*;
import com.tune.picturebackend.model.entity.User;
import com.tune.picturebackend.model.vo.user.LoginUserInfoVO;
import com.tune.picturebackend.model.vo.user.LoginUserVO;
import com.tune.picturebackend.model.vo.user.UserVO;
import com.tune.picturebackend.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

/**
 * @Author: Tune
 * @Description:用户模块
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;

    /**
     * 用户注册
     */
    @SaIgnore
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody @Validated UserRegisterRequest userRegisterRequest) {
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录（返回脱敏信息）
     */
    @SaIgnore
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody @Validated UserLoginRequest userLoginRequest) {
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户，并返回脱敏后的用户信息
     * @return
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser() {
        User loginUser = userService.getLoginUser();
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 退出登录
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<String> userLogout() {
        StpUtil.logout();
        return ResultUtils.success("退出成功");
    }

    /**
     * 新增用户（管理员）
     * @param userAddRequest
     * @return 新增用户的 id
     */
    @PostMapping("/add")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    public BaseResponse<Long> addUser(@RequestBody @Validated UserAddRequest userAddRequest) {
        Long userId = userService.addUser(userAddRequest);
        return ResultUtils.success(userId);
    }

    /**
     * 更新用户（管理员）
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    public BaseResponse<Boolean> updateUser(@RequestBody @Validated UserUpdateRequest userUpdateRequest) {
        boolean updateResult = userService.updateUser(userUpdateRequest);
        return ResultUtils.success(updateResult);
    }
    /**
     * 更新用户（个人）
     * @param userUpdateByLoginUserRequest
     * @return
     */
    @PostMapping("/update/owner")
    public BaseResponse<Boolean> updateLoginUserInfo(@RequestBody @Validated UserUpdateByLoginUserRequest userUpdateByLoginUserRequest) {
        boolean updateResult = userService.updateLoginUserInfo(userUpdateByLoginUserRequest);
        return ResultUtils.success(updateResult);
    }

    /**
     * 删除用户（逻辑删除）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    public BaseResponse<Boolean> deleteUser(@RequestBody @Validated DeleteRequest deleteRequest) {
        boolean deleteResult = userService.deleteUser(deleteRequest);
        return ResultUtils.success(deleteResult);
    }

    /**
     * 根据 id 获取用户（管理员）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    public BaseResponse<UserVO> getUserVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 根据 id 获取用户包装类 （用户个人查看）
     * @param id
     * @return
     */
    @GetMapping("/get/owner")
    public BaseResponse<LoginUserInfoVO> getLoginUserInfoVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(userService.getLoginUserInfoVO(user));
    }

    /**
     * 分页获取用户列表（管理员）
     * @param userQueryRequest
     * @return
     */
    @PostMapping("/list/page/vo")
    @SaCheckRole(value = {"admin", "root"}, mode = SaMode.OR)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody @Valid UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();
        // 分页查询得到的完整用户列表
        Page<User> userPage = userService.page(new Page<>(current, size), userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage  = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}