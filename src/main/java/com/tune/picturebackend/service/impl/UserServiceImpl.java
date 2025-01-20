package com.tune.picturebackend.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.constant.UserConstant;
import com.tune.picturebackend.model.dto.user.UserAddRequest;
import com.tune.picturebackend.model.dto.user.UserUpdateByLoginUserRequest;
import com.tune.picturebackend.model.dto.user.UserUpdateRequest;
import com.tune.picturebackend.exception.BusinessException;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.model.dto.user.UserQueryRequest;
import com.tune.picturebackend.model.entity.User;
import com.tune.picturebackend.model.vo.user.LoginUserInfoVO;
import com.tune.picturebackend.model.vo.user.LoginUserVO;
import com.tune.picturebackend.model.vo.user.UserVO;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.service.UserService;
import com.tune.picturebackend.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;


import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
* @author Tune
* @description 针对表【user(用户)】的数据库操作Service实现
* @createDate 2025-01-03 14:02:03
*/
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User>
    implements UserService{

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        // 密码、确认密码是否相同
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入密码不一致");
        }
        // 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        //注册成功的账号默认自动登录，直接缓存该对象
        StpUtil.login(user.getId());
        User loginUser = this.getById(user.getId());
        StpUtil.getSession().set("loginUser", loginUser);

        return user.getId();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword) {
        // 2.密码加密查询
        String encryptPassword = getEncryptPassword(userPassword);
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 登录认证和缓存登录对象
        StpUtil.login(user.getId());
        StpUtil.getSession().set("loginUser", user);

        // 3.返回用户信息脱敏
        return getLoginUserVO(user);
    }

    @Override
    public User getLoginUser() {
        User currentUser = (User)StpUtil.getSession().get("loginUser");
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return currentUser;
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 加盐，混淆密码
        final String salt = "salt123";
        return DigestUtils.md5DigestAsHex((salt + userPassword).getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public LoginUserInfoVO getLoginUserInfoVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserInfoVO loginUserInfoVO = new LoginUserInfoVO();
        BeanUtil.copyProperties(user, loginUserInfoVO);
        return loginUserInfoVO;
    }

    @Override
    public boolean updateLoginUserInfo(UserUpdateByLoginUserRequest userUpdateByLoginUserRequest) {
        User user = this.getById(userUpdateByLoginUserRequest.getId());
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR, "用户不存在,请联系管理员");
        BeanUtil.copyProperties(userUpdateByLoginUserRequest, user);
        boolean updateResult = this.updateById(user);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);
        // 更新缓存
        StpUtil.getSession().set("loginUser", user);
        return true;
    }

    @Override
    public Long addUser(UserAddRequest userAddRequest) {
        // 管理员默认只能添加普通用户
        boolean isOperatorAdmin = StpUtil.hasRole(UserConstant.ADMIN_ROLE);
        boolean addUserRoleDefaultCheck = userAddRequest.getUserRole().equals(UserConstant.DEFAULT_ROLE);
        ThrowUtils.throwIf(isOperatorAdmin && !addUserRoleDefaultCheck, ErrorCode.NO_AUTH_ERROR);

        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        // 密码加密
        user.setUserPassword(getEncryptPassword(user.getUserPassword()));
        boolean saveResult = this.save(user);
        ThrowUtils.throwIf(!saveResult, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public Boolean updateUser(UserUpdateRequest userUpdateRequest) {
        // 操作对象用户
        User user = this.getById(userUpdateRequest.getId());

        // 管理员只能操作普通用户
        boolean isOperatorAdmin = StpUtil.hasRole(UserConstant.ADMIN_ROLE);
        boolean isTargetDefaultUser = user.getUserRole().equals(UserConstant.DEFAULT_ROLE);
        ThrowUtils.throwIf(isOperatorAdmin && !isTargetDefaultUser, ErrorCode.NO_AUTH_ERROR);
        // 管理员禁止修改用户角色
        boolean updateUserRoleCheck = userUpdateRequest.getUserRole().equals(user.getUserRole());
        ThrowUtils.throwIf(isOperatorAdmin && !updateUserRoleCheck, ErrorCode.NO_AUTH_ERROR);

        // 更新用户
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean updateResult = this.updateById(user);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR);

        return true;
    }

    @Override
    public Boolean deleteUser(DeleteRequest deleteRequest) {
        // 操作对象用户
        User user = this.getById(deleteRequest.getId());
        // 管理员只能操作普通用户
        boolean isOperatorAdmin = StpUtil.hasRole(UserConstant.ADMIN_ROLE);
        boolean isTargetDefaultUser = user.getUserRole().equals(UserConstant.DEFAULT_ROLE);
        ThrowUtils.throwIf(isOperatorAdmin && !isTargetDefaultUser, ErrorCode.NO_AUTH_ERROR);

        // 创建 UpdateWrapper 进行自定义更新
        UpdateWrapper<User> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", deleteRequest.getId());
        // 设置 isDelete 字段为 1，进行逻辑删除
        updateWrapper.set("isDelete", 1);
        // 设置 del_unique_key 字段为当前删除记录的 id
        updateWrapper.set("del_unique_key", deleteRequest.getId());

        // 假设你有一个 userMapper 可以调用 update 方法，例如：
        return this.update(null, updateWrapper);
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        // return userList.stream().map(user -> getUserVO(user)).collect(Collectors.toList());
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        Integer gender = userQueryRequest.getGender();
        String phone = userQueryRequest.getPhone();
        String email = userQueryRequest.getEmail();
        Integer userStatus = userQueryRequest.getUserStatus();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.eq(ObjectUtil.isNotNull(gender), "gender", gender);
        queryWrapper.like(StrUtil.isNotBlank(phone), "phone", phone);
        queryWrapper.eq(StrUtil.isNotBlank(email), "email", email);
        queryWrapper.eq(ObjectUtil.isNotNull(userStatus), "userStatus", userStatus);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }
}




