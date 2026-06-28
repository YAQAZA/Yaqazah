//package com.yaqazah.infrastructure.config;
//
//import com.yaqazah.infrastructure.interceptor.DeviceInterceptor;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class WebMvcConfig implements WebMvcConfigurer {
//
//    private final DeviceInterceptor deviceInterceptor;
//
//    // Spring constructor injection handles pulling the component from the interceptor package
//    public WebMvcConfig(DeviceInterceptor deviceInterceptor) {
//        this.deviceInterceptor = deviceInterceptor;
//    }
//
//    @Override
//    public void addInterceptors(InterceptorRegistry registry) {
//        // This targets all endpoints. Adjust patterns if you want to exclude static assets.
//        registry.addInterceptor(deviceInterceptor).addPathPatterns("/**");
////                .excludePathPatterns();
//    }
//
//
//}