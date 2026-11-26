package com.bookworm.service;

import com.bookworm.dto.Dtos.*;
import com.bookworm.exception.ApiException;
import com.bookworm.model.User;
import com.bookworm.repository.UserRepository;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Registration, login and opaque bearer-token sessions.
 * Tokens are kept in memory, so they are invalidated on restart (fine for this capstone).
 */
@Service
public class AuthService {
    private final UserRepository users;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Long> sessions = new ConcurrentHashMap<>();

    public AuthService(UserRepository users) {
        this.users = users;
    }

    public AuthResponse register(RegisterRequest r) {
        if (users.findByEmailIgnoreCase(r.email()).isPresent()) {
            throw ApiException.conflict("Email already registered");
        }
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(r.email().trim().toLowerCase());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setGiftPoints(100); // welcome bonus
        users.save(u);
        return new AuthResponse(newSession(u), UserDto.of(u));
    }

    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmailIgnoreCase(r.email().trim())
                .filter(x -> encoder.matches(r.password(), x.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return new AuthResponse(newSession(u), UserDto.of(u));
    }

    public void logout(String token) {
        if (token != null) sessions.remove(token);
    }

    public Optional<User> userForToken(String token) {
        if (token == null) return Optional.empty();
        Long id = sessions.get(token);
        return id == null ? Optional.empty() : users.findById(id);
    }

    private String newSession(User u) {
        byte[] b = new byte[32];
        random.nextBytes(b);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
        sessions.put(token, u.getId());
        return token;
    }
}
