package com.careersphere.service;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.Skill;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.SkillRepository;
import com.careersphere.repository.UserRepository;
import java.net.URI;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileService {
    private final CurrentUserService currentUser;
    private final UserRepository users;
    private final SkillRepository skills;

    public ProfileService(CurrentUserService currentUser, UserRepository users, SkillRepository skills) {
        this.currentUser = currentUser;
        this.users = users;
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public Dto.UserProfile getMe() { return toDto(currentUser.requireUser()); }

    @Transactional
    public Dto.UserProfile update(Dto.ProfileUpdate input) {
        User user = currentUser.requireUser();
        if (input.fullName() != null) user.fullName = input.fullName().trim();
        if (input.phone() != null) user.phone = clean(input.phone());
        if (input.location() != null) user.location = clean(input.location());
        if (input.education() != null) user.education = clean(input.education());
        if (input.graduationYear() != null) user.graduationYear = input.graduationYear();
        if (input.targetRole() != null) user.targetRole = clean(input.targetRole());
        if (input.experienceLevel() != null) user.experienceLevel = input.experienceLevel();
        if (input.githubUrl() != null) user.githubUrl = validatedUrl(input.githubUrl());
        if (input.linkedinUrl() != null) user.linkedinUrl = validatedUrl(input.linkedinUrl());
        if (input.skills() != null) {
            user.skills.clear();
            for (String name : input.skills()) {
                String trimmed = name.trim();
                if (trimmed.isEmpty()) continue;
                Skill skill = skills.findByNameIgnoreCase(trimmed).orElseGet(() -> skills.save(new Skill(trimmed, "Technical")));
                user.skills.add(skill);
            }
        }
        return toDto(users.save(user));
    }

    @Transactional
    public Dto.MessageResponse changePassword(Dto.ChangePasswordRequest input, org.springframework.security.crypto.password.PasswordEncoder encoder) {
        User user = currentUser.requireUser();
        if (!encoder.matches(input.currentPassword(), user.passwordHash)) {
            throw ApiException.badRequest("Your current password is incorrect.");
        }
        if (encoder.matches(input.newPassword(), user.passwordHash)) {
            throw ApiException.badRequest("Choose a new password you have not used before.");
        }
        user.passwordHash = encoder.encode(input.newPassword());
        users.save(user);
        return new Dto.MessageResponse("Password changed. Sign in again on other devices.");
    }

    public Dto.UserProfile toDto(User user) {
        List<String> names = user.skills == null ? List.of() : user.skills.stream()
            .map(skill -> skill.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        return new Dto.UserProfile(user.id, user.fullName, user.email, user.phone, user.location, user.education,
            user.graduationYear, user.targetRole, user.experienceLevel, names, user.githubUrl, user.linkedinUrl, user.createdAt);
    }

    private String clean(String value) { return value.isBlank() ? null : value.trim(); }

    private String validatedUrl(String value) {
        if (value.isBlank()) return null;
        try {
            URI uri = URI.create(value.trim());
            if (!("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                || uri.getHost() == null) throw new IllegalArgumentException();
            return uri.toString();
        } catch (Exception exception) {
            throw ApiException.badRequest("Enter a valid GitHub or LinkedIn URL.");
        }
    }
}
