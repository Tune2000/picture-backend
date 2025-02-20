package com.tune.picturebackend.controller.picture;

import cn.dev33.satoken.annotation.SaCheckRole;
import cn.dev33.satoken.annotation.SaMode;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tune.picturebackend.api.aliyunai.AliYunAiApi;
import com.tune.picturebackend.api.aliyunai.model.CreateOutPaintingTaskResponse;
import com.tune.picturebackend.api.aliyunai.model.GetOutPaintingTaskResponse;
import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.common.ResultUtils;
import com.tune.picturebackend.constant.UserConstant;
import com.tune.picturebackend.exception.BusinessException;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.model.dto.picture.*;
import com.tune.picturebackend.model.entity.Picture;
import com.tune.picturebackend.model.vo.picture.LocalAvatarUploadVO;
import com.tune.picturebackend.model.vo.picture.PictureTagCategory;
import com.tune.picturebackend.model.vo.picture.PictureVO;
import com.tune.picturebackend.service.PictureService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

/**
 * @Author: Tune
 * @Description:图片模块
 */
@RestController
@RequestMapping("/picture")
@Slf4j
public class PictureController {

    @Resource
    private PictureService pictureService;

    @Resource
    private AliYunAiApi aliYunAiApi;
    /**
     * 上传头像
     *
     * @param multipartFile
     * @return
     */
    @PostMapping("/uploadAvatar")
    public BaseResponse<LocalAvatarUploadVO> uploadAvatar(@RequestPart("file") MultipartFile multipartFile) {
        // 上传头像（存腾讯云COS）
        LocalAvatarUploadVO localAvatarUploadVO = pictureService.uploadAvatar(multipartFile);
        return ResultUtils.success(localAvatarUploadVO);

        // 上传头像（存本地）
        // String url = fileManager.uploadLocalAvatar(file);
        // LocalAvatarUploadVO localAvatarUploadVO = new LocalAvatarUploadVO();
        // localAvatarUploadVO.setAvatarUrl(url);
        // return ResultUtils.success(localAvatarUploadVO);
    }

    /**
     * 通过 URL 上传图片（可重新上传）
     */
    @PostMapping("/upload/url")
    public BaseResponse<PictureVO> uploadPictureByUrl(@RequestBody PictureUploadRequest pictureUploadRequest) {
        String fileUrl = pictureUploadRequest.getFileUrl();
        PictureVO pictureVO = pictureService.uploadPicture(fileUrl, pictureUploadRequest);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 上传图片（可重新上传）
     */
    @PostMapping("/upload")
    public BaseResponse<PictureVO> uploadPicture(@RequestPart("file") MultipartFile multipartFile, PictureUploadRequest pictureUploadRequest) {
        PictureVO pictureVO = pictureService.uploadPicture(multipartFile, pictureUploadRequest);
        return ResultUtils.success(pictureVO);
    }

    /**
     * 删除图片
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deletePicture(@RequestBody @Valid DeleteRequest deleteRequest) {
        pictureService.deletePicture(deleteRequest);
        return ResultUtils.success(true);
    }

    /**
     * 更新图片（仅管理员可用）
     */
    @PostMapping("/update")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Boolean> updatePicture(@RequestBody @Valid PictureUpdateRequest pictureUpdateRequest) {
        boolean result = pictureService.updatePicture(pictureUpdateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 编辑图片（给用户使用）
     */
    @PostMapping("/edit")
    public BaseResponse<Boolean> editPicture(@RequestBody @Valid PictureEditRequest pictureEditRequest) {
        pictureService.editPicture(pictureEditRequest);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取图片（仅管理员可用）
     */
    @GetMapping("/get")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Picture> getPictureById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 获取封装类
        return ResultUtils.success(picture);
    }

    /**
     * 根据 id 获取图片（封装类）
     */
    @GetMapping("/get/vo")
    public BaseResponse<PictureVO> getPictureVOById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        // 查询数据库
        Picture picture = pictureService.getById(id);
        ThrowUtils.throwIf(picture == null, ErrorCode.NOT_FOUND_ERROR);
        // 空间权限校验
        pictureService.checkPictureAuth(picture);
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVO(picture));
    }

    /**
     * 分页获取图片列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Page<Picture>> listPictureByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        return ResultUtils.success(picturePage);
    }

    /**
     * 分页获取图片列表（封装类）
     */
    @PostMapping("/list/page/vo")
    public BaseResponse<Page<PictureVO>> listPictureVOByPage(@RequestBody PictureQueryRequest pictureQueryRequest) {
        long current = pictureQueryRequest.getCurrent();
        long size = pictureQueryRequest.getPageSize();
        // 限制爬虫
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);
        // 空间权限校验
        pictureService.checkSpaceAuth(pictureQueryRequest);
        // 查询数据库
        Page<Picture> picturePage = pictureService.page(new Page<>(current, size), pictureService.getQueryWrapper(pictureQueryRequest));
        // 获取封装类
        return ResultUtils.success(pictureService.getPictureVOPage(picturePage));
    }

    /**
     * 分页获取图片列表（封装类，缓存优化）
     */
    @Deprecated
    @PostMapping("/list/page/vo/cache")
    public BaseResponse<Page<PictureVO>> listPictureVOByPageWithCache(@RequestBody PictureQueryRequest pictureQueryRequest) {
        Page<PictureVO> pictureVOPage = pictureService.getPictureVoPageWithCache(pictureQueryRequest);
        return ResultUtils.success(pictureVOPage);
    }

    @GetMapping("/tag_category")
    public BaseResponse<PictureTagCategory> listPictureTagCategory() {
        PictureTagCategory pictureTagCategory = new PictureTagCategory();
        List<String> tagList = Arrays.asList("热门", "搞笑", "生活", "高清", "艺术", "校园", "背景", "简历", "创意");
        List<String> categoryList = Arrays.asList("模板", "电商", "表情包", "素材", "海报");
        pictureTagCategory.setTagList(tagList);
        pictureTagCategory.setCategoryList(categoryList);
        return ResultUtils.success(pictureTagCategory);
    }

    @PostMapping("/review")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Boolean> doPictureReview(@RequestBody @Valid PictureReviewRequest pictureReviewRequest) {
        pictureService.doPictureReview(pictureReviewRequest);
        return ResultUtils.success(true);
    }

    @PostMapping("/upload/batch")
    @SaCheckRole(value = {UserConstant.ADMIN_ROLE, UserConstant.ROOT_ROLE}, mode = SaMode.OR)
    public BaseResponse<Integer> uploadPictureByBatch(@RequestBody PictureUploadByBatchRequest pictureUploadByBatchRequest) {
        ThrowUtils.throwIf(pictureUploadByBatchRequest == null, ErrorCode.PARAMS_ERROR);
        int uploadCount = pictureService.uploadPictureByBatch(pictureUploadByBatchRequest);
        return ResultUtils.success(uploadCount);
    }

    /**
     * 根据颜色搜索图片
     */
    @PostMapping("/search/color")
    public BaseResponse<List<PictureVO>> searchPictureByColor(@RequestBody @Valid SearchPictureByColorRequest searchPictureByColorRequest) {
        String picColor = searchPictureByColorRequest.getPicColor();
        Long spaceId = searchPictureByColorRequest.getSpaceId();
        List<PictureVO> result = pictureService.searchPictureByColor(spaceId, picColor);
        return ResultUtils.success(result);
    }

    /**
     * 批量编辑图片
     */
    @PostMapping("/edit/batch")
    public BaseResponse<Boolean> editPictureByBatch(@RequestBody @Valid PictureEditByBatchRequest pictureEditByBatchRequest) {
        pictureService.editPictureByBatch(pictureEditByBatchRequest);
        return ResultUtils.success(true);
    }

    /**
     * 创建 AI 扩图任务
     */
    @PostMapping("/out_painting/create_task")
    public BaseResponse<CreateOutPaintingTaskResponse> createPictureOutPaintingTask(
            @RequestBody CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest) {
        if (createPictureOutPaintingTaskRequest == null || createPictureOutPaintingTaskRequest.getPictureId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        CreateOutPaintingTaskResponse response = pictureService.createPictureOutPaintingTask(createPictureOutPaintingTaskRequest);
        return ResultUtils.success(response);
    }

    /**
     * 查询 AI 扩图任务
     */
    @GetMapping("/out_painting/get_task")
    public BaseResponse<GetOutPaintingTaskResponse> getPictureOutPaintingTask(String taskId) {
        ThrowUtils.throwIf(StrUtil.isBlank(taskId), ErrorCode.PARAMS_ERROR);
        GetOutPaintingTaskResponse task = aliYunAiApi.getOutPaintingTask(taskId);
        return ResultUtils.success(task);
    }
}
