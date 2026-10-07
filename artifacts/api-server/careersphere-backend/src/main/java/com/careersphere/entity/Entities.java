package com.careersphere.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class Entities {
    private Entities() {}

    @Entity
    @Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
    public static class User {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @Column(nullable = false, length = 120)
        public String fullName;
        @Column(nullable = false, length = 254)
        public String email;
        @Column(nullable = false, length = 100)
        public String passwordHash;
        @Column(length = 30)
        public String phone;
        @Column(length = 120)
        public String location;
        @Column(length = 180)
        public String education;
        public Integer graduationYear;
        @Column(length = 120)
        public String targetRole;
        @Column(nullable = false, length = 32)
        public String experienceLevel = "Fresher";
        @Column(length = 500)
        public String githubUrl;
        @Column(length = 500)
        public String linkedinUrl;
        @Column(nullable = false, updatable = false)
        public LocalDateTime createdAt = LocalDateTime.now();
        @ManyToMany
        @JoinTable(name = "user_skills", joinColumns = @JoinColumn(name = "user_id"),
                inverseJoinColumns = @JoinColumn(name = "skill_id"))
        public Set<Skill> skills = new HashSet<>();
    }

    @Entity
    @Table(name = "skills", uniqueConstraints = @UniqueConstraint(name = "uk_skills_name", columnNames = "name"))
    public static class Skill {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @Column(nullable = false, length = 80)
        public String name;
        @Column(nullable = false, length = 48)
        public String category;
        protected Skill() {}
        public Skill(String name, String category) { this.name = name; this.category = category; }
    }

    @Entity
    @Table(name = "job_roles")
    public static class JobRole {
        @Id @Column(length = 80)
        public String id;
        @Column(nullable = false, length = 120)
        public String name;
        @ManyToMany
        @JoinTable(name = "job_role_skills", joinColumns = @JoinColumn(name = "job_role_id"),
                inverseJoinColumns = @JoinColumn(name = "skill_id"))
        public Set<Skill> requiredSkills = new HashSet<>();
        protected JobRole() {}
        public JobRole(String id, String name) { this.id = id; this.name = name; }
    }

    @Entity
    @Table(name = "resumes")
    public static class Resume {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        public User user;
        @Column(nullable = false, length = 255)
        public String fileName;
        @Column(nullable = false, length = 120)
        public String targetRole;
        @Lob @Column(nullable = false)
        public byte[] pdfContent;
        @Lob @Column(nullable = false)
        public String extractedText;
        @Column(nullable = false, updatable = false)
        public LocalDateTime createdAt = LocalDateTime.now();
        @OneToOne(mappedBy = "resume", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        public ResumeAnalysis analysis;
    }

    @Entity
    @Table(name = "resume_analysis")
    public static class ResumeAnalysis {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "resume_id", nullable = false, unique = true)
        public Resume resume;
        @Column(nullable = false)
        public int atsScore;
        @Column(nullable = false)
        public int overallScore;
        @Convert(converter = ListJsonConverter.class) @Column(length = 12000)
        public List<String> technicalSkills = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> softSkills = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> missingSkills = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> strengths = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> weaknesses = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 12000)
        public List<String> suggestions = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> recommendedTechnologies = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> recommendedRoles = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> keywords = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 6000)
        public List<String> projectSuggestions = new ArrayList<>();
        @Column(nullable = false, length = 16)
        public String assessmentMode = "DEMO";
        @Column(length = 500)
        public String assessmentNote;
    }

    @Entity
    @Table(name = "interviews")
    public static class Interview {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false)
        public User user;
        @Column(nullable = false, length = 120)
        public String role;
        @Column(nullable = false, length = 32)
        public String experienceLevel;
        @Column(nullable = false, length = 16)
        public String difficulty;
        @Column(nullable = false)
        public int questionCount;
        @Column(nullable = false, length = 16)
        public String status = "IN_PROGRESS";
        public Double overallScore;
        @Convert(converter = ListJsonConverter.class) @Column(length = 4000)
        public List<String> strengths = new ArrayList<>();
        @Convert(converter = ListJsonConverter.class) @Column(length = 4000)
        public List<String> weaknesses = new ArrayList<>();
        @Column(nullable = false, updatable = false)
        public LocalDateTime createdAt = LocalDateTime.now();
        @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("questionNumber ASC")
        public List<InterviewQuestion> questions = new ArrayList<>();
        @Column(nullable = false, length = 16)
        public String assessmentMode = "DEMO";
        @Column(length = 500)
        public String assessmentNote;
    }

    @Entity
    @Table(name = "interview_questions", uniqueConstraints = @UniqueConstraint(
            name = "uk_interview_question_number", columnNames = {"interview_id", "question_number"}))
    public static class InterviewQuestion {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "interview_id", nullable = false)
        public Interview interview;
        @Column(name = "question_number", nullable = false)
        public int questionNumber;
        @Column(nullable = false, length = 48)
        public String category;
        @Column(nullable = false, length = 1200)
        public String prompt;
        @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
        public InterviewAnswer answer;
    }

    @Entity
    @Table(name = "interview_answers")
    public static class InterviewAnswer {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "question_id", nullable = false, unique = true)
        public InterviewQuestion question;
        @Lob @Column(nullable = false)
        public String answerText;
        @Column(nullable = false)
        public double score;
        @Column(nullable = false)
        public double technicalCorrectness;
        @Column(nullable = false)
        public double completeness;
        @Column(nullable = false)
        public double clarity;
        @Column(nullable = false, columnDefinition = "DOUBLE DEFAULT 0.0")
        public double relevance;
        @Column(nullable = false)
        public double communication;
        @Lob @Column(nullable = false)
        public String feedback;
        @Column(nullable = false, length = 16)
        public String assessmentMode = "DEMO";
    }

    @Entity
    @Table(name = "study_plans", uniqueConstraints = @UniqueConstraint(name = "uk_study_plan_user", columnNames = "user_id"))
    public static class StudyPlan {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @OneToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "user_id", nullable = false, unique = true)
        public User user;
        @Column(nullable = false, length = 120)
        public String targetRole;
        @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
        @OrderBy("orderIndex ASC")
        public List<StudyTopic> topics = new ArrayList<>();
    }

    @Entity
    @Table(name = "study_topics")
    public static class StudyTopic {
        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        public Long id;
        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "study_plan_id", nullable = false)
        public StudyPlan plan;
        @Column(nullable = false)
        public int week;
        @Column(nullable = false, length = 120)
        public String title;
        @Column(nullable = false, length = 800)
        public String description;
        @Column(nullable = false)
        public boolean completed;
        @Column(name = "order_index", nullable = false)
        public int orderIndex;
    }
}
