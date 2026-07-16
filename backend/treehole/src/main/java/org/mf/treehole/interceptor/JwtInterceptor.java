package org.mf.treehole.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mf.treehole.common.Constants;
import org.mf.treehole.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendError(response, "未登录", HttpStatus.UNAUTHORIZED.value());
            return false;
        }

        String token = authHeader.substring(7);
        try {
            if (jwtUtil.isExpired(token)) {
                sendError(response, "登录已过期，请重新登录", HttpStatus.UNAUTHORIZED.value());
                return false;
            }

            if (Boolean.TRUE.equals(redisTemplate.hasKey(Constants.REDIS_JWT_BLACKLIST + token))) {
                sendError(response, "登录已失效，请重新登录", HttpStatus.UNAUTHORIZED.value());
                return false;
            }

            Claims claims = jwtUtil.parseToken(token);
            request.setAttribute("userId", Long.parseLong(claims.getSubject()));
            request.setAttribute("email", claims.get("email", String.class));
            request.setAttribute("role", claims.get("role", String.class));
            request.setAttribute("token", token);
            return true;
        } catch (Exception e) {
            sendError(response, "无效的Token", HttpStatus.UNAUTHORIZED.value());
            return false;
        }
    }

    private void sendError(HttpServletResponse response, String message, int status) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(mapper.writeValueAsString(Map.of("code", status, "message", message, "data", "")));
    }
}
