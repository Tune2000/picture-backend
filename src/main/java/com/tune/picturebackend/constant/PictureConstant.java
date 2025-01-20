package com.tune.picturebackend.constant;

/**
 * @Author: Tune
 * @Description:
 * @CreateTime: 2025-01-15
 */

public interface PictureConstant {
    // 定义允许的文件大小，这里假设最大为 5MB
    long MAX_FILE_SIZE = 5 * 1024 * 1024;

    // 定义允许的文件类型
    String[] ALLOWED_FILE_TYPES = {"image/jpeg", "image/png"};
}
