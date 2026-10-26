package com.funding.config;

import com.funding.common.JwtInterceptor;
import com.funding.common.RoleInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 * 注册 JwtInterceptor + RoleInterceptor，配置 CORS
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Autowired
    private RoleInterceptor roleInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JwtInterceptor 必须先执行（设置 UserContext），RoleInterceptor 后执行
        // 白名单：登录接口 + 扫码公开核验接口（第三方监事/审计无需登录，仅只读公开数据）
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/login", "/api/public/**");
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/api/auth/login", "/api/public/**");
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 允许全部跨域（Demo 简化）
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
