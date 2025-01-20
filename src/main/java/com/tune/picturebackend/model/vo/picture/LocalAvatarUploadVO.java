package com.tune.picturebackend.model.vo.picture;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: Tune
 * @Description: 本地头像上传地址返回
 */
@Data
public class LocalAvatarUploadVO implements Serializable {
    // 头像地址
    private String avatarUrl;
}
