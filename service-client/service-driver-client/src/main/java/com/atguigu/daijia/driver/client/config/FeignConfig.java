//package com.atguigu.daijia.driver.client.config;
//
//import com.atguigu.daijia.common.util.AuthContextHolder;
//import feign.RequestInterceptor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//@Configuration
//public class FeignConfig {
//    @Bean
//    public RequestInterceptor requestInterceptor() {
//        return requestTemplate -> {
//            Long userId = AuthContextHolder.getUserId();
//            if (userId != null) {
//                // 将userId添加到请求头
//                requestTemplate.header("userId", String.valueOf(userId));
//            }
//        };
//    }
//}