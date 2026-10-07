package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
public class AccountController {
    private final ProfileService profiles;
    private final PasswordEncoder passwords;
    public AccountController(ProfileService profiles, PasswordEncoder passwords) { this.profiles = profiles; this.passwords = passwords; }

    @GetMapping("/users/me")
    public Dto.UserProfile getProfile() { return profiles.getMe(); }

    @PutMapping("/users/me")
    public Dto.UserProfile updateProfile(@Valid @RequestBody Dto.ProfileUpdate request) { return profiles.update(request); }

    @PostMapping("/auth/change-password")
    public ResponseEntity<Dto.MessageResponse> changePassword(@Valid @RequestBody Dto.ChangePasswordRequest request) {
        return ResponseEntity.ok(profiles.changePassword(request, passwords));
    }
}
