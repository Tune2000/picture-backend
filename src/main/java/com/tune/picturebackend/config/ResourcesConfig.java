package com.tune.picturebackend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Author: Tune
 * @Description:
 * @CreateTime: 2025-01-16
 */
@Configuration
public class ResourcesConfig implements WebMvcConfigurer {

    @Autowired
    private ProjectConfig projectConfig;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry)
    {
        // 所有/avatar/开头的请求 都会去后面配置的路径下查找资源
        registry.addResourceHandler("/avatar/**")
                .addResourceLocations("file:" + projectConfig.getProfile() + "/");
    }
}
