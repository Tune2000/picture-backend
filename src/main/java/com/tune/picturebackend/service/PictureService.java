package com.tune.picturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.tune.picturebackend.common.DeleteRequest;
import com.tune.picturebackend.model.dto.picture.*;
import com.tune.picturebackend.model.entity.Picture;
import com.tune.picturebackend.model.vo.picture.LocalAvatarUploadVO;
import com.tune.picturebackend.model.vo.picture.PictureVO;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author Tune
 * @description 针对表【picture(图片)】的数据库操作Service
 */
public interface PictureService extends IService<Picture> {
    /**
     * 上传图片
     *
     * @param inputSource 文件输入源
     * @param pictureUploadRequest
     * @return
     */
    PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest);

    /**
     * 上传头像
     *
     * @param multipartFile
     * @return
     */
    LocalAvatarUploadVO uploadAvatar(MultipartFile multipartFile);

    /**
     * 获取查询条件（图片）
     *
     * @param pictureQueryRequest
     * @return
     */
    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    /**
     * 获取单个图片封装
     *
     * @param picture
     * @return
     */
    PictureVO getPictureVO(Picture picture);

    /**
     * 获取图片分页封装
     *
     * @param picturePage
     * @return
     */
    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage);

    /**
     * 获取图片分页（缓存优化）
     *
     * @param pictureQueryRequest
     * @return
     */
    Page<PictureVO> getPictureVoPageWithCache(PictureQueryRequest pictureQueryRequest);

    /**
     * 验证图片
     *
     * @param picture
     */
    void validPicture(Picture picture);

    /**
     * 删除图片
     *
     * @param deleteRequest
     * @return
     */
    boolean deletePicture(DeleteRequest deleteRequest);

    /**
     * 更新图片
     *
     * @param pictureUpdateRequest
     * @return
     */
    boolean updatePicture(PictureUpdateRequest pictureUpdateRequest);

    /**
     * 编辑图片（用户）
     *
     * @param pictureEditRequest
     * @return
     */
    boolean editPicture(PictureEditRequest pictureEditRequest);

    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest);

    /**
     * 填充审核参数
     *
     * @param picture
     */
    void fillReviewParams(Picture picture);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @return 成功创建的图片数
     */
    int uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest);

    /**
     * 清理图片文件
     *
     * @param oldPicture
     */
    void clearPictureFile(Picture oldPicture);
}
