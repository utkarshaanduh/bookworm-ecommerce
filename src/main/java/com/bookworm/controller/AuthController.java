package com.bookworm.controller;

import com.bookworm.config.AuthInterceptor;
import com.bookworm.dto.Dtos.*;
import com.bookworm.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) {
        return auth.register(r);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) {
        return auth.login(r);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest req) {
        auth.logout((String) req.getAttribute(AuthInterceptor.TOKEN_ATTR));
    }

    @GetMapping("/me")
    public UserDto me(HttpServletRequest req) {
        return UserDto.of(AuthInterceptor.requireUser(req));
    }
}
