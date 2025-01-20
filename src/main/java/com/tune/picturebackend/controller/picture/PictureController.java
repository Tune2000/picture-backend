package com.tune.picturebackend.controller.picture;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.tune.picturebackend.common.BaseResponse;
import com.tune.picturebackend.common.ResultUtils;
import com.tune.picturebackend.config.ProjectConfig;
import com.tune.picturebackend.exception.ErrorCode;
import com.tune.picturebackend.exception.ThrowUtils;
import com.tune.picturebackend.model.vo.picture.LocalAvatarUploadVO;
import com.tune.picturebackend.model.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

import static com.tune.picturebackend.constant.PictureConstant.ALLOWED_FILE_TYPES;
import static com.tune.picturebackend.constant.PictureConstant.MAX_FILE_SIZE;

/**
 * @Author: Tune
 * @Description:图片模块
 */
@RestController
@RequestMapping("/picture")
public class PictureController {

    @Autowired
    private ProjectConfig projectConfig;


    /**
     * 本地上传头像
     * @param file
     * @return
     */
    @PostMapping("/uploadAvatar")
    public BaseResponse<LocalAvatarUploadVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        // 检查文件是否为空
        ThrowUtils.throwIf(file.isEmpty(), ErrorCode.OPERATION_ERROR,"上传文件为空");
        // 检查文件大小是否超出限制
        ThrowUtils.throwIf(file.getSize() > MAX_FILE_SIZE, ErrorCode.OPERATION_ERROR, "文件大小超出限制");
        // 检查文件类型是否允许
        boolean allowedFileType = false;
        for (String type : ALLOWED_FILE_TYPES) {
            if (Objects.equals(file.getContentType(), type)) {
                allowedFileType = true;
                break;
            }
        }
        ThrowUtils.throwIf(!allowedFileType, ErrorCode.OPERATION_ERROR, "文件类型不被允许上传");

        // "example.jpg" --> "jpg"
        String fileExtension = FileUtil.extName(file.getOriginalFilename());
        // "jpg" --> "随机UUID.jpg"
        String uniqueFileName = IdUtil.fastSimpleUUID() + "." + fileExtension;

        // 本地根路径
        // "D:/avatar/uploadPath"
        String uploadPath = projectConfig.getProfile();

        User loginUser = (User) StpUtil.getSession().get("loginUser");
        String FilePath = loginUser.getId().toString();
        // 完整路径
        // "D:/avatar/uploadPath/用户id命名文件夹"
        String fullUploadPath = uploadPath + File.separator + FilePath;

        File uploadDir = new File(fullUploadPath);
        if (!uploadDir.exists()) {
            FileUtil.mkdir(uploadDir);
        }

        // 保存文件
        // "D:/avatar/uploadPath/用户id/随机UUID.jpg"
        File dest = new File(fullUploadPath + File.separator + uniqueFileName);
        try {
            FileUtil.writeBytes(file.getBytes(), dest);
        } catch (IOException e) {
            ThrowUtils.throwIf(true, ErrorCode.OPERATION_ERROR, "文件保存失败：" + e.getMessage());
        }

        // 生成访问图片的URL
        // /avatar/用户id/随机UUID.jpg (/avatar是ResourcesConfig配置中的静态资源映射地址)
        String url = "/avatar/" + FilePath + "/" + uniqueFileName;
        LocalAvatarUploadVO localAvatarUploadVO = new LocalAvatarUploadVO();
        localAvatarUploadVO.setAvatarUrl(url);

        // 返回结果
        return ResultUtils.success(localAvatarUploadVO);
    }
}
