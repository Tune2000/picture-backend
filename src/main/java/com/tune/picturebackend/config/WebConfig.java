package com.tune.picturebackend.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String[] exclude_path = {
            "/**/doc.*",
            "/**/swagger-ui.*",
            "/**/swagger-resources",
            "/**/webjars/**",
            "/**/v2/api-docs/**",
            "/**/login",
            "/**/register"
    };

    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
        registry.addInterceptor(new SaInterceptor(handler ->
                        SaRouter.match("/**", r -> StpUtil.checkLogin()))
                        .isAnnotation(true))
                .addPathPatterns("/**")
                .excludePathPatterns(exclude_path);
    }


}