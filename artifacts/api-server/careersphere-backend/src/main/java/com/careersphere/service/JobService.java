package com.careersphere.service;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.JobRole;
import com.careersphere.entity.Entities.Skill;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.JobRoleRepository;
import com.careersphere.repository.SkillRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JobService {
    private final CurrentUserService currentUser;
    private final JobRoleRepository roles;
    private final SkillRepository skills;
    public JobService(CurrentUserService currentUser, JobRoleRepository roles, SkillRepository skills) {
        this.currentUser = currentUser;
        this.roles = roles;
        this.skills = skills;
    }

    @Transactional(readOnly = true)
    public List<Dto.Skill> listSkills() {
        return skills.findAll().stream().sorted(Comparator.comparing(skill -> skill.name, String.CASE_INSENSITIVE_ORDER))
            .map(skill -> new Dto.Skill(skill.name, skill.category)).toList();
    }

    @Transactional(readOnly = true)
    public List<Dto.JobRole> listRoles() {
        return roles.findAll().stream().sorted(Comparator.comparing(role -> role.name, String.CASE_INSENSITIVE_ORDER))
            .map(role -> new Dto.JobRole(role.id, role.name, role.requiredSkills.stream()
                .map(skill -> skill.name).sorted(String.CASE_INSENSITIVE_ORDER).toList())).toList();
    }

    @Transactional(readOnly = true)
    public Dto.JobMatch match(Dto.JobMatchRequest request) {
        JobRole role = roles.findById(request.role()).orElseGet(() -> roles.findAll().stream()
            .filter(item -> item.name.equalsIgnoreCase(request.role())).findFirst()
            .orElseThrow(() -> ApiException.notFound("Choose one of the available job roles.")));
        List<String> required = role.requiredSkills.stream().map(skill -> skill.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        Set<String> normalized = new LinkedHashSet<>();
        request.skills().stream().filter(value -> value != null).map(JobService::normalize).forEach(normalized::add);
        List<String> matching = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String skill : required) (normalized.contains(normalize(skill)) ? matching : missing).add(skill);
        int percentage = required.isEmpty() ? 0 : (int) Math.round(100.0 * matching.size() / required.size());
        return new Dto.JobMatch(role.name, percentage, matching, missing, missing);
    }

    public List<String> getUserSkillNames(User user) {
        return user.skills.stream().map(skill -> skill.name).sorted(String.CASE_INSENSITIVE_ORDER).toList();
    }

    private static String normalize(String value) {
        return value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }
}
