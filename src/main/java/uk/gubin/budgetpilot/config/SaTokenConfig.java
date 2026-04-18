package uk.gubin.budgetpilot.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import jakarta.servlet.DispatcherType;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gubin.budgetpilot.filter.ApiKeyFilter;
import uk.gubin.budgetpilot.mapper.UserMapper;

@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/v1/auth/login"
                );
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // SPA fallback: 所有非 API 路由返回 index.html
        registry.addViewController("/{spring:[^\\.]*}")
                .setViewName("forward:/index.html");
    }

    @Bean
    public FilterRegistrationBean<ApiKeyFilter> apiKeyFilter(UserMapper userMapper) {
        FilterRegistrationBean<ApiKeyFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ApiKeyFilter(userMapper));
        registration.addUrlPatterns("/api/v1/*");
        registration.setName("apiKeyFilter");
        registration.setOrder(-1); // 在 SaToken 过滤器之前执行
        return registration;
    }
}
