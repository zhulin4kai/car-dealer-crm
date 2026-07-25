package com.autodealer.crm.bootstrap.security;


import com.autodealer.crm.shared.web.CorsConfig;
import com.autodealer.crm.bootstrap.security.TokenVerifyFilter;
import com.autodealer.crm.bootstrap.security.MyAccessDeniedHandler;
import com.autodealer.crm.bootstrap.security.MyAuthenticationFailureHandler;
import com.autodealer.crm.bootstrap.security.MyAuthenticationSuccessHandler;
import com.autodealer.crm.bootstrap.security.MyLogoutSuccessHandler;
import com.autodealer.crm.shared.security.SecurityPaths;
import com.autodealer.crm.shared.infrastructure.constants.Constants;
import jakarta.annotation.Resource;
import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;

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

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
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
                    authorize.dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
                            .requestMatchers(HttpMethod.POST, SecurityPaths.LOGIN).permitAll()
                            .requestMatchers(SecurityPaths.ERROR).permitAll()
                            .requestMatchers(SecurityPaths.INTERNAL_AI_TOOLS).permitAll()
                            .requestMatchers(HttpMethod.POST, SecurityPaths.CREDENTIAL_ACTIVATE,
                                    SecurityPaths.CREDENTIAL_FORGOT, SecurityPaths.CREDENTIAL_RESET,
                                    SecurityPaths.CREDENTIAL_VERIFY_CONTACT,
                                    SecurityPaths.BREAK_GLASS_REQUEST,
                                    SecurityPaths.BREAK_GLASS_COMPLETE).permitAll()
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                            .anyRequest().authenticated(); // 其它任何请求都需要登录后才能访问
                })

                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())

                .sessionManagement((session) -> {
                    // session 创建策略
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS); // 无session状态，也就是禁用session
                })

                // 添加自定义的 Filter
                .addFilterBefore(tokenVerifyFilter, LogoutFilter.class)

                // 退出登录
                .logout((logout) -> {
                    logout.logoutUrl(SecurityPaths.LOGOUT)
                            .logoutSuccessHandler(myLogoutSuccessHandler);
                })

                // 这个是没有权限访问时触发
                .exceptionHandling((exceptionHandling) -> {
                    exceptionHandling.accessDeniedHandler(myAccessDeniedHandler);
                })

                .build();
    }

    // CORS configuration is handled by CorsConfig.java to avoid duplication
}
