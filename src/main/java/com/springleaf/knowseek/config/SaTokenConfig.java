package com.springleaf.knowseek.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册Sa-Token的注解拦截器，拦截所有路径，但放行登录注册请求
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 登录校验 -- 拦截所有路径，并排除登录注册接口
            SaRouter.match("/**")
                    .notMatch("/user/login", "/user/register", "/error")
                    .check(r -> StpUtil.checkLogin());
            
            // 角色校验 -- 管理员接口
            SaRouter.match("/org/create", "/org/assign", "/org/addSub", "/org/update", "/org/list")
                    .check(r -> StpUtil.checkRole("admin"));
        }))
        .addPathPatterns("/**")
        .excludePathPatterns(
                "/user/login",
                "/user/register",
                "/error"
        );
    }
}