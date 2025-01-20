package com.tune.picturebackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: Tune
 * @Description:项目配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "project")
public class ProjectConfig {
    // 项目名称
    private String name;
    // 项目版本
    private String version;
    // 图片上传地址
    private String profile;
}
