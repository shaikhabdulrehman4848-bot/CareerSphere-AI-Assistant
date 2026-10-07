package com.careersphere.service;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.UserRepository;
import com.careersphere.security.JwtTokenService;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtTokenService tokens;
    private final ProfileService profiles;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtTokenService tokens, ProfileService profiles) {
        this.users = users;
        this.encoder = encoder;
        this.tokens = tokens;
        this.profiles = profiles;
    }

    @Transactional
    public Dto.UserProfile register(Dto.RegisterRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) throw ApiException.conflict("An account with this email already exists.");
        User user = new User();
        user.fullName = request.fullName().trim();
        user.email = email;
        user.passwordHash = encoder.encode(request.password());
        user.phone = emptyToNull(request.phone());
        user.location = emptyToNull(request.location());
        user.education = emptyToNull(request.education());
        user.graduationYear = request.graduationYear();
        user.targetRole = emptyToNull(request.targetRole());
        user.experienceLevel = request.experienceLevel() == null ? "Fresher" : request.experienceLevel();
        return profiles.toDto(users.save(user));
    }

    @Transactional(readOnly = true)
    public User authenticate(Dto.LoginRequest request) {
        User user = users.findByEmailIgnoreCase(request.email().trim())
            .orElseThrow(() -> ApiException.unauthorized("Email or password is incorrect."));
        if (!encoder.matches(request.password(), user.passwordHash)) {
            throw ApiException.unauthorized("Email or password is incorrect.");
        }
        return user;
    }

    public String issueToken(User user) { return tokens.issue(user.email); }
    public long tokenTtlSeconds() { return tokens.getTtlSeconds(); }
    public String emptyToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
