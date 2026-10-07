package com.careersphere.service;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.Interview;
import com.careersphere.entity.Entities.InterviewQuestion;
import com.careersphere.entity.Entities.Resume;
import com.careersphere.entity.Entities.StudyPlan;
import com.careersphere.entity.Entities.StudyTopic;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.InterviewRepository;
import com.careersphere.repository.ResumeRepository;
import com.careersphere.repository.StudyPlanRepository;
import com.careersphere.repository.StudyTopicRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudyPlanService {
    private final CurrentUserService currentUser;
    private final StudyPlanRepository plans;
    private final StudyTopicRepository topics;
    private final ResumeRepository resumes;
    private final InterviewRepository interviews;

    public StudyPlanService(CurrentUserService currentUser, StudyPlanRepository plans, StudyTopicRepository topics,
                            ResumeRepository resumes, InterviewRepository interviews) {
        this.currentUser = currentUser;
        this.plans = plans;
        this.topics = topics;
        this.resumes = resumes;
        this.interviews = interviews;
    }

    @Transactional
    public Dto.StudyPlan get() {
        User user = currentUser.requireUser();
        StudyPlan plan = plans.findByUserId(user.id).orElseGet(() -> createPlan(user));
        String role = user.targetRole == null || user.targetRole.isBlank() ? "Java Backend Developer" : user.targetRole;
        if (!plan.targetRole.equals(role) && plan.topics.stream().noneMatch(topic -> topic.completed)) plan.targetRole = role;
        return toDto(plans.save(plan));
    }

    @Transactional
    public Dto.StudyTopic update(long topicId, Dto.StudyTopicUpdate request) {
        User user = currentUser.requireUser();
        StudyTopic topic = topics.findByIdAndPlanUserId(topicId, user.id)
            .orElseThrow(() -> ApiException.notFound("Study topic not found."));
        topic.completed = request.completed();
        topic = topics.save(topic);
        return toTopicDto(topic);
    }

    private StudyPlan createPlan(User user) {
        StudyPlan plan = new StudyPlan();
        plan.user = user;
        plan.targetRole = user.targetRole == null || user.targetRole.isBlank() ? "Java Backend Developer" : user.targetRole;
        List<String> gaps = resumes.findByUserIdOrderByCreatedAtDesc(user.id).stream()
            .filter(resume -> resume.analysis != null).findFirst().map(resume -> resume.analysis.missingSkills).orElse(List.of());
        String weakest = weakestTopic(interviews.findByUserIdOrderByCreatedAtDesc(user.id));
        List<String> titles = new ArrayList<>(List.of("Core Java & OOP", "Collections & exception handling",
            "Multithreading fundamentals", "SQL, DBMS & MySQL", "Spring Boot & JPA",
            "REST APIs, validation & testing", "Spring Security & JWT", "Docker & deployment"));
        if (weakest != null && !weakest.isBlank()) titles.set(0, "Focused review: " + weakest);
        for (int index = 0; index < Math.min(3, gaps.size()); index++) {
            if (!titles.get(0).toLowerCase().contains(gaps.get(index).toLowerCase())) titles.set(index + 1, "Skill gap: " + gaps.get(index));
        }
        for (int index = 0; index < titles.size(); index++) {
            StudyTopic topic = new StudyTopic();
            topic.plan = plan;
            topic.week = index + 1;
            topic.orderIndex = index + 1;
            topic.title = titles.get(index);
            topic.description = description(titles.get(index), plan.targetRole);
            topic.completed = false;
            plan.topics.add(topic);
        }
        return plans.save(plan);
    }

    private String weakestTopic(List<Interview> sessions) {
        return sessions.stream().filter(interview -> "COMPLETED".equals(interview.status))
            .flatMap(interview -> interview.questions.stream()).filter(question -> question.answer != null)
            .min(java.util.Comparator.comparingDouble(question -> question.answer.score))
            .map(question -> question.category).orElse(null);
    }

    private String description(String title, String role) {
        if (title.toLowerCase().contains("focused review")) return "Revisit concepts and practice an interview answer on this topic for your " + role + " goal.";
        if (title.startsWith("Skill gap:")) return "Build evidence for this skill through a small Java backend exercise, tests, and a concise project note.";
        return switch (title) {
            case "Core Java & OOP" -> "Refresh classes, interfaces, composition, generics, and the JVM; explain one tradeoff with an example.";
            case "Collections & exception handling" -> "Practice collection choice, hashing, iteration, custom exceptions, and clear error handling.";
            case "Multithreading fundamentals" -> "Review threads, executors, synchronization, and safe shared state; write a small concurrent example.";
            case "SQL, DBMS & MySQL" -> "Practice joins, indexes, transactions, constraints, and query plans against a small normalized schema.";
            case "Spring Boot & JPA" -> "Build a layered service with DTOs, validation, repositories, transactions, and JPA relationships.";
            case "REST APIs, validation & testing" -> "Design status codes and request validation, then add focused service and controller tests.";
            case "Spring Security & JWT" -> "Implement password hashing, protected routes, token expiry, and safe error responses.";
            default -> "Package your project, configure environment variables, document deployment, and rehearse the implementation.";
        };
    }

    private Dto.StudyPlan toDto(StudyPlan plan) {
        int total = plan.topics.size();
        int done = (int) plan.topics.stream().filter(topic -> topic.completed).count();
        int percent = total == 0 ? 0 : Math.round(100f * done / total);
        return new Dto.StudyPlan(plan.targetRole, percent, plan.topics.stream().map(this::toTopicDto).toList());
    }

    private Dto.StudyTopic toTopicDto(StudyTopic topic) {
        return new Dto.StudyTopic(topic.id, topic.week, topic.title, topic.description, topic.completed, topic.orderIndex);
    }
}
