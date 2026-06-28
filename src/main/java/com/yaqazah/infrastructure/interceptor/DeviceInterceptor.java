//package com.yaqazah.infrastructure.interceptor;
//
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.stereotype.Component;
//import org.springframework.web.servlet.HandlerInterceptor;
//
//@Component
//public class DeviceInterceptor implements HandlerInterceptor {
//
//    @Override
//    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
//        String mobileHint = request.getHeader("Sec-CH-UA-Mobile");
//        String userAgent = request.getHeader("User-Agent");
//        String platformHint = request.getHeader("Sec-CH-UA-Platform");
//
//        boolean isMobile = "?1".equals(mobileHint) ||
//                (userAgent != null && userAgent.toLowerCase().contains("mobile"));
//
//        String platform = "Unknown OS";
//        if (platformHint != null) {
//            platform = platformHint.replace("\"", "");
//        } else if (userAgent != null) {
//            String uaLower = userAgent.toLowerCase();
//            if (uaLower.contains("windows")) platform = "Windows";
//            else if (uaLower.contains("android")) platform = "Android";
//            else if (uaLower.contains("iphone") || uaLower.contains("ipad")) platform = "iOS";
//            else if (uaLower.contains("macintosh")) platform = "macOS";
//            else if (uaLower.contains("linux")) platform = "Linux";
//        }
//
//        request.setAttribute("isMobile", isMobile);
//        request.setAttribute("devicePlatform", platform);
//
//        return true;
//    }
//}