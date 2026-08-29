package com.teach.user.security;

import io.jsonwebtoken.Claims;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 轻量鉴权拦截器（未引入完整 Spring Security）：
 * - 公开路径（/api/auth/**、/actuator/**、/error）放行；
 * - 优先信任网关透传的 X-User-Id / X-User-Role 头（内部与网关场景）；
 * - 否则尝试解析 Authorization: Bearer <JWT>。
 * 结果写入 IdentityContext，供控制器读取。
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    public AuthInterceptor(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        if (isPublic(path)) return true;

        UserIdentity identity = resolve(request);
        if (identity == null) {
            response.setStatus(401);
            return false;
        }
        IdentityContext.set(identity);
        response.setHeader("X-User-Id", String.valueOf(identity.getUserId()));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        IdentityContext.clear();
    }

    private boolean isPublic(String path) {
        return path.startsWith("/api/auth")
                || path.startsWith("/api/version")
                || path.startsWith("/actuator")
                || path.startsWith("/error");
    }

    private UserIdentity resolve(HttpServletRequest request) {
        // 网关/内部透传的身份头
        String xid = request.getHeader("X-User-Id");
        if (xid != null && !xid.trim().isEmpty()) {
            try {
                Long userId = Long.valueOf(xid.trim());
                String role = request.getHeader("X-User-Role");
                String username = request.getHeader("X-User-Name");
                return new UserIdentity(userId, username, role);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        // JWT
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            try {
                Claims claims = jwtUtil.parse(auth.substring(7));
                Long userId = Long.valueOf(claims.getSubject());
                return new UserIdentity(userId,
                        claims.get("username", String.class),
                        claims.get("role", String.class));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
