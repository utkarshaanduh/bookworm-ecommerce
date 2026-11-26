package com.bookworm.config;

import com.bookworm.exception.ApiException;
import com.bookworm.model.User;
import com.bookworm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Resolves the Bearer token to a user. Endpoints needing a login call {@link #requireUser}. */
@Component
public class AuthInterceptor implements HandlerInterceptor {
    public static final String USER_ATTR = "bookworm.user";
    public static final String TOKEN_ATTR = "bookworm.token";

    private final AuthService auth;

    public AuthInterceptor(AuthService auth) {
        this.auth = auth;
    }

    @Override
    public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {
        String h = req.getHeader("Authorization");
        if (h != null && h.regionMatches(true, 0, "Bearer ", 0, 7)) {
            String token = h.substring(7).trim();
            Optional<User> u = auth.userForToken(token);
            u.ifPresent(user -> {
                req.setAttribute(USER_ATTR, user);
                req.setAttribute(TOKEN_ATTR, token);
            });
        }
        return true;
    }

    public static User requireUser(HttpServletRequest req) {
        Object u = req.getAttribute(USER_ATTR);
        if (u == null) throw ApiException.unauthorized("Login required");
        return (User) u;
    }

    public static User optionalUser(HttpServletRequest req) {
        return (User) req.getAttribute(USER_ATTR);
    }
}
