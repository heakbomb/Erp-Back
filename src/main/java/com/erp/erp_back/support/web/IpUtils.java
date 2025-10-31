package com.erp.erp_back.support.web;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {
    public static String getClientIp(HttpServletRequest request) {
        String[] headerKeys = {
            "X-Forwarded-For", "X-Real-IP", "CF-Connecting-IP",
            "Proxy-Client-IP", "WL-Proxy-Client-IP"
        };
        for (String key : headerKeys) {
            String v = request.getHeader(key);
            if (v != null && !v.isBlank() && !"unknown".equalsIgnoreCase(v)) {
                // X-Forwarded-For: client, proxy1, proxy2 ...
                int comma = v.indexOf(',');
                return comma > 0 ? v.substring(0, comma).trim() : v.trim();
            }
        }
        String ip = request.getRemoteAddr();
        return ip == null ? "" : ip;
    }
}