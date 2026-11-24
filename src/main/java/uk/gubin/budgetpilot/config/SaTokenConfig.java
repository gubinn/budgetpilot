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
                        "/api/v1/auth/login",
                        "/api/v1/auth/verify",
                        "/api/v1/auth/otp/resend",
                        "/api/v1/auth/ingress",
                        // 只读分享页免登录；写保护由 ReadonlyFilter 负责
                        "/api/v1/shares/public/**"
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
        // 只读分享链接是两段路径，需要单独兜底
        registry.addViewController("/share/**")
                .setViewName("forward:/index.html");
        // 交易编辑是多段路径；刷新/直接打开时也要回到 SPA，而不是 404 JSON
        registry.addViewController("/transactions/edit/**")
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
