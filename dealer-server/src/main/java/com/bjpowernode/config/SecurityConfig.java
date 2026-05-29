package com.bjpowernode.config;

import com.bjpowernode.config.filter.TokenVerifyFilter;
import com.bjpowernode.config.handler.MyAccessDeniedHandler;
import com.bjpowernode.config.handler.MyAuthenticationFailureHandler;
import com.bjpowernode.config.handler.MyAuthenticationSuccessHandler;
import com.bjpowernode.config.handler.MyLogoutSuccessHandler;
import com.bjpowernode.constant.Constants;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@EnableMethodSecurity // 开启方法级别的权限检查
@Configuration
public class SecurityConfig {

    @Resource
    private MyAuthenticationSuccessHandler myAuthenticationSuccessHandler;

    @Resource
    private MyAuthenticationFailureHandler myAuthenticationFailureHandler;

    @Resource
    private MyLogoutSuccessHandler myLogoutSuccessHandler;

    @Resource
    private MyAccessDeniedHandler myAccessDeniedHandler;

    @Resource
    private TokenVerifyFilter tokenVerifyFilter;

    @Bean // There is no PasswordEncoder mapped for the id "null"
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity,
            CorsConfigurationSource configurationSource) throws Exception {
        // 禁用跨站请求伪造
        return httpSecurity
                .formLogin((formLogin) -> {
                    formLogin.loginProcessingUrl(Constants.LOGIN_URI) // 登录处理地址，不需要写Controller
                            .usernameParameter("loginAct")
                            .passwordParameter("loginPwd")
                            .successHandler(myAuthenticationSuccessHandler)
                            .failureHandler(myAuthenticationFailureHandler);
                })

                .authorizeHttpRequests((authorize) -> {
                    authorize.requestMatchers("/api/login").permitAll()
                            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll() // 允许所有OPTIONS请求
                            .anyRequest().authenticated(); // 其它任何请求都需要登录后才能访问
                })

                .csrf(AbstractHttpConfigurer::disable) // 方法引用，禁用跨站请求伪造

                // 支持跨域请求
                .cors((cors) -> {
                    cors.configurationSource(configurationSource);
                })

                .sessionManagement((session) -> {
                    // session创建策略
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 无session状态，也就是禁用session
                })

                // 添加自定义的Filter
                .addFilterBefore(tokenVerifyFilter, LogoutFilter.class)

                // 退出登录
                .logout((logout) -> {
                    logout.logoutUrl("/api/logout") // 退出提交到该地址，该地址不需要我们写controller的，是框架处理
                            .logoutSuccessHandler(myLogoutSuccessHandler);
                })

                // 这个是没有权限访问时触发
                .exceptionHandling((exceptionHandling) -> {
                    exceptionHandling.accessDeniedHandler(myAccessDeniedHandler);
                })

                .build();
    }

    @Bean
    @Primary
    public CorsConfigurationSource configurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*")); // 允许任何来源，http://localhost:8081
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS")); // 明确允许的方法
        configuration.setAllowedHeaders(Arrays.asList("*")); // 允许任何的请求头

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
