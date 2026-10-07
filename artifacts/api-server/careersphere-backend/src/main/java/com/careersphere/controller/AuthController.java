package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.User;
import com.careersphere.security.JwtAuthenticationFilter;
import com.careersphere.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService auth;
    private final boolean secureCookie;

    public AuthController(AuthService auth, @Value("${app.security.cookie-secure:true}") boolean secureCookie) {
        this.auth = auth;
        this.secureCookie = secureCookie;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) { return Map.of("token", token.getToken()); }

    @PostMapping("/register")
    public ResponseEntity<Dto.AuthResponse> register(@Valid @RequestBody Dto.RegisterRequest request) {
        Dto.UserProfile profile = auth.register(request);
        User user = new User();
        user.email = profile.email();
        return ResponseEntity.status(201).header(HttpHeaders.SET_COOKIE, sessionCookie(auth.issueToken(user), auth.tokenTtlSeconds()).toString())
            .body(new Dto.AuthResponse(profile));
    }

    @PostMapping("/login")
    public ResponseEntity<Dto.AuthResponse> login(@Valid @RequestBody Dto.LoginRequest request) {
        User user = auth.authenticate(request);
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, sessionCookie(auth.issueToken(user), auth.tokenTtlSeconds()).toString())
            .body(new Dto.AuthResponse(new Dto.UserProfile(
                user.id, user.fullName, user.email, user.phone, user.location, user.education, user.graduationYear,
                user.targetRole, user.experienceLevel,
                user.skills.stream().map(skill -> skill.name).sorted(String.CASE_INSENSITIVE_ORDER).toList(),
                user.githubUrl, user.linkedinUrl, user.createdAt
            )));
    }

    @PostMapping("/logout")
    public ResponseEntity<Dto.MessageResponse> logout() {
        ResponseCookie cleared = ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, "")
            .httpOnly(true).secure(secureCookie).path("/api").sameSite("Lax").maxAge(0).build();
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cleared.toString())
            .body(new Dto.MessageResponse("You have been signed out."));
    }

    private ResponseCookie sessionCookie(String token, long ttl) {
        return ResponseCookie.from(JwtAuthenticationFilter.COOKIE_NAME, token)
            .httpOnly(true).secure(secureCookie).path("/api").sameSite("Lax").maxAge(ttl).build();
    }
}
