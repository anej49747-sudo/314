package com.chef.chef.config;

import java.io.File;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目运行根目录下的 uploads 绝对路径
        String uploadPath = new File("uploads").getAbsolutePath() + File.separator;

        // 映射：当访问 http://ip:8080/uploads/** 时，去本地磁盘的 uploads 文件夹去找文件
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath);
    }
}