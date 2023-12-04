//package com.mrc.community.config;
//
//import com.mrc.community.util.CommunityConstant;
//
//import com.mrc.community.util.CommunityUtil;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import org.springframework.security.access.AccessDeniedException;
//import org.springframework.security.config.Customizer;
//import org.springframework.security.config.annotation.web.builders.HttpSecurity;
//import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
//import org.springframework.security.core.AuthenticationException;
//import org.springframework.security.web.AuthenticationEntryPoint;
//import org.springframework.security.web.SecurityFilterChain;
//import org.springframework.security.web.access.AccessDeniedHandler;
//
//import java.io.IOException;
//import java.io.PrintWriter;
//
//
//@Configuration
//public class SecurityConfig implements CommunityConstant {
//
//    //放行静态资源
//    @Bean
//    public WebSecurityCustomizer webSecurityCustomizer(){
//        return (web) -> web.ignoring().requestMatchers("/resources/**");
//    }
//
//    @Bean
//    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception{
//        http.authorizeHttpRequests(authorize->{
//            try{
//                //放行登录接口
//                authorize.requestMatchers(
//                        "/user/setting",
//                        "/user/upload",
//                        "/discuss/add",
//                        "/comment/add/**",
//                        "/letter/**",
//                        "/notice/**",
//                        "/like",
//                        "/follow",
//                        "/unfollow"
//                )
//                        .hasAnyAuthority(
//                                AUTHORITY_USER,
//                                AUTHORITY_ADMIN,
//                                AUTHORITY_MODERATOR
//                        )
//                        .requestMatchers(
//                                "/discuss/top",
//                                "/discuss/wonderful"
//                        )
//                        .hasAnyAuthority(
//                                AUTHORITY_MODERATOR
//                        )
//                        .requestMatchers(
//                                "/discuss/delete",
//                                "/data/**",
//                                "/actuator/**"
//                        )
//                        .hasAnyAuthority(
//                                AUTHORITY_ADMIN
//                        )
//                        .requestMatchers("").permitAll()
//                        //其余的都需要权限校验
//                        .anyRequest().authenticated();
//            }catch(Exception e){
//                throw new RuntimeException(e);
//            }
//        }
//        );
//        //防跨站请求伪造
//        http.csrf(Customizer.withDefaults());
//
//        // 权限不够时的处理
//        http.exceptionHandling()
//                .authenticationEntryPoint(new AuthenticationEntryPoint() {
//                    @Override
//                    public void commence(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, AuthenticationException authException) throws IOException, jakarta.servlet.ServletException {
//                        String xRequestedWith = request.getHeader("x-requested-with");
//                        if ("XMLHttpRequest".equals(xRequestedWith)) {
//                            response.setContentType("application/plain;charset=utf-8");
//                            PrintWriter writer = response.getWriter();
//                            writer.write(CommunityUtil.getJSONString(403, "你还没有登录哦!"));
//                        } else {
//                            response.sendRedirect(request.getContextPath() + "/login");
//                        }
//                    }
//                })
//                .accessDeniedHandler(new AccessDeniedHandler() {
//                    @Override
//                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
//                        String xRequestedWith = request.getHeader("x-requested-with");
//                        if ("XMLHttpRequest".equals(xRequestedWith)) {
//                            response.setContentType("application/plain;charset=utf-8");
//                            PrintWriter writer = response.getWriter();
//                            writer.write(CommunityUtil.getJSONString(403, "你没有访问此功能的权限!"));
//                        } else {
//                            response.sendRedirect(request.getContextPath() + "/denied");
//                        }
//                    }
//                });
//
//        // Security底层默认会拦截/logout请求,进行退出处理.
//        // 覆盖它默认的逻辑,才能执行我们自己的退出代码.
//        http.logout().logoutUrl("/securitylogout");
//
//
//        return http.build();
//    }
//
//
//
//}
