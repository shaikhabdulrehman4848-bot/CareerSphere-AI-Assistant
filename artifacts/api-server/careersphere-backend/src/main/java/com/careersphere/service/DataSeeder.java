package com.careersphere.service;

import com.careersphere.entity.Entities.JobRole;
import com.careersphere.entity.Entities.Skill;
import com.careersphere.repository.JobRoleRepository;
import com.careersphere.repository.SkillRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataSeeder implements CommandLineRunner {
    private final SkillRepository skills;
    private final JobRoleRepository roles;
    public DataSeeder(SkillRepository skills, JobRoleRepository roles) { this.skills = skills; this.roles = roles; }

    @Override
    @Transactional
    public void run(String... args) {
        Map<String, String> catalog = new LinkedHashMap<>();
        for (String value : List.of("Java|Language", "OOP|Foundation", "Data Structures|Foundation",
            "Spring Boot|Framework", "Spring Security|Framework", "REST APIs|Backend", "SQL|Database",
            "MySQL|Database", "JPA & Hibernate|Persistence", "Git|Tools", "Maven|Tools", "Docker|Cloud",
            "AWS|Cloud", "Collections|Language", "Multithreading|Language", "JUnit|Testing",
            "Microservices|Architecture", "Problem Solving|Soft skill", "Communication|Soft skill")) {
            String[] bits = value.split("\\|");
            catalog.put(bits[0], bits[1]);
        }
        Map<String, Skill> known = new LinkedHashMap<>();
        catalog.forEach((name, category) -> known.put(name, skills.findByNameIgnoreCase(name)
            .orElseGet(() -> skills.save(new Skill(name, category)))));

        Map<String, List<String>> roleSkills = Map.of(
            "java-developer", List.of("Java", "OOP", "Collections", "SQL", "Git", "Maven", "JUnit"),
            "java-backend-developer", List.of("Java", "Spring Boot", "REST APIs", "Spring Security", "SQL", "MySQL", "JPA & Hibernate", "Git"),
            "software-engineer", List.of("Java", "OOP", "Data Structures", "SQL", "REST APIs", "Git", "JUnit"),
            "spring-boot-developer", List.of("Java", "Spring Boot", "REST APIs", "Spring Security", "SQL", "JPA & Hibernate", "Maven")
        );
        Map<String, String> names = Map.of(
            "java-developer", "Java Developer",
            "java-backend-developer", "Java Backend Developer",
            "software-engineer", "Software Engineer",
            "spring-boot-developer", "Spring Boot Developer"
        );
        roleSkills.forEach((id, requirements) -> {
            JobRole role = roles.findById(id).orElseGet(() -> new JobRole(id, names.get(id)));
            role.name = names.get(id);
            role.requiredSkills = new LinkedHashSet<>(requirements.stream().map(known::get).toList());
            roles.save(role);
        });
    }
}
