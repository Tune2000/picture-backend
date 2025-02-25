package com.tune.picturebackend.controller.space;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.common.ResultUtils;
import com.tune.picturebackend.constant.UserConstant;
import com.tune.picturebackend.enums.SpaceLevelEnum;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.manager.auth.SpaceUserAuthManager;
import com.tune.picturebackend.model.dto.space.*;
import com.tune.picturebackend.model.entity.Space;
import com.tune.picturebackend.model.vo.space.SpaceVO;
import com.tune.picturebackend.service.SpaceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: Tune
 * @Description: 空间模块
 */
@RestController
@RequestMapping("/space")
@Slf4j
public class SpaceController {
    @Resource
    private SpaceService spaceService;

    @Resource
    private SpaceUserAuthManager spaceUserAuthManager;

    @PostMapping("/add")
    public BaseResponse<Long> addSpace(@RequestBody @Valid SpaceAddRequest spaceAddRequest) {
        long spaceId = spaceService.addSpace(spaceAddRequest);
        return ResultUtils.success(spaceId);
    }

    @PostMapping("/update")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Boolean> updateSpace(@RequestBody @Valid SpaceUpdateRequest spaceUpdateRequest) {
        spaceService.updateSpace(spaceUpdateRequest);
        return ResultUtils.success(true);
    }

    /**
     * 编辑空间（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editSpace(@RequestBody SpaceEditRequest spaceEditRequest) {
        spaceService.editSpace(spaceEditRequest);
        return ResultUtils.success(true);
    }

    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSpace(@RequestBody @Valid DeleteRequest deleteRequest) {
        spaceService.deleteSpace(deleteRequest);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取空间（仅管理员可用）
     */
    @GetMapping("/get")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Space> getSpaceById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(space);
    }

    /**
     * 根据 id 获取空间（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<SpaceVO> getSpaceVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Space space = spaceService.getById(id);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        SpaceVO spaceVO = spaceService.getSpaceVO(space);
        List<String> permissionList = spaceUserAuthManager.getPermissionList(space);
        spaceVO.setPermissionList(permissionList);

        return ResultUtils.success(spaceVO);
    }

    /**
     * 分页获取空间列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Page<Space>> listSpaceByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        return ResultUtils.success(spacePage);
    }
    /**
     * 分页获取空间列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<SpaceVO>> listSpaceVOByPage(@RequestBody SpaceQueryRequest spaceQueryRequest) {
        long current = spaceQueryRequest.getCurrent();
        long size = spaceQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Page<Space> spacePage = spaceService.page(new Page<>(current, size), spaceService.getQueryWrapper(spaceQueryRequest));
        // 获取封装类
        return ResultUtils.success(spaceService.getSpaceVOPage(spacePage));
    }

    @GetMapping("/list/level")
    public BaseResponse<List<SpaceLevel>> listSpaceLevel() {
        List<SpaceLevel> spaceLevelList = Arrays.stream(SpaceLevelEnum.values()) // 获取所有枚举
                .map(spaceLevelEnum -> new SpaceLevel(
                        spaceLevelEnum.getValue(),
                        spaceLevelEnum.getText(),
                        spaceLevelEnum.getMaxCount(),
                        spaceLevelEnum.getMaxSize()))
                .collect(Collectors.toList());
        return ResultUtils.success(spaceLevelList);
    }

}
