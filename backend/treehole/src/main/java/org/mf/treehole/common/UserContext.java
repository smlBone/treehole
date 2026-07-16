package org.mf.treehole.common;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class UserContext {

    public static Long getUserId() {
        HttpServletRequest request = getRequest();
        if (request != null) {
            Object userId = request.getAttribute("userId");
            if (userId instanceof Long) {
                return (Long) userId;
            }
        }
        return null;
    }

    public static String getEmail() {
        HttpServletRequest request = getRequest();
        return request != null ? (String) request.getAttribute("email") : null;
    }

    public static String getRole() {
        HttpServletRequest request = getRequest();
        return request != null ? (String) request.getAttribute("role") : null;
    }

    public static String getToken() {
        HttpServletRequest request = getRequest();
        return request != null ? (String) request.getAttribute("token") : null;
    }

    public static boolean isAdmin() {
        return Constants.ROLE_ADMIN.equals(getRole());
    }

    public static boolean isReviewer() {
        return Constants.ROLE_REVIEWER.equals(getRole()) || isAdmin();
    }

    private static HttpServletRequest getRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }
}
