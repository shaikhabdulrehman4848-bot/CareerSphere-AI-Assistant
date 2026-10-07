package com.careersphere.service;

import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;
    public CurrentUserService(UserRepository users) { this.users = users; }

    public User requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null
            || "anonymousUser".equals(authentication.getPrincipal())) {
            throw ApiException.unauthorized("Please sign in to continue.");
        }
        return users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> ApiException.unauthorized("Your session has expired. Please sign in again."));
    }
}
