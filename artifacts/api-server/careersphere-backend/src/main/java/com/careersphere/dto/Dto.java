package com.careersphere.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public final class Dto {
    private Dto() {}

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 10, max = 72) String password,
        @Size(max = 30) String phone,
        @Size(max = 120) String location,
        @Size(max = 180) String education,
        @Min(1980) @Max(2100) Integer graduationYear,
        @Size(max = 120) String targetRole,
        @Pattern(regexp = "Fresher|Entry-level|Mid-level", message = "Choose a valid experience level") String experienceLevel
    ) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
    public record ChangePasswordRequest(@NotBlank String currentPassword, @NotBlank @Size(min = 10, max = 72) String newPassword) {}
    public record AuthResponse(UserProfile user) {}
    public record MessageResponse(String message) {}
    public record ApiError(int status, String message, LocalDateTime timestamp, Map<String, String> fieldErrors) {}
    public record UserProfile(
        Long id, String fullName, String email, String phone, String location, String education,
        Integer graduationYear, String targetRole, String experienceLevel, List<String> skills,
        String githubUrl, String linkedinUrl, LocalDateTime createdAt
    ) {}
    public record ProfileUpdate(
        @Size(min = 2, max = 120) String fullName,
        @Size(max = 30) String phone,
        @Size(max = 120) String location,
        @Size(max = 180) String education,
        @Min(1980) @Max(2100) Integer graduationYear,
        @Size(max = 120) String targetRole,
        @Pattern(regexp = "Fresher|Entry-level|Mid-level") String experienceLevel,
        @Size(max = 40) List<@Size(max = 64) String> skills,
        @Size(max = 500) String githubUrl,
        @Size(max = 500) String linkedinUrl
    ) {}

    public record InterviewSummary(
        Long id, String role, String difficulty, int questionCount, int answeredCount,
        String status, Double overallScore, List<String> strengths, List<String> weaknesses,
        LocalDateTime createdAt
    ) {}
    public record InterviewQuestion(
        Long id, int questionNumber, String category, String prompt, boolean answered,
        String answer, Double score, Double technicalCorrectness, Double completeness,
        Double clarity, Double relevance, Double communication, String feedback
    ) {}
    public record InterviewDetail(
        Long id, String role, String difficulty, int questionCount, int answeredCount,
        String status, Double overallScore, List<String> strengths, List<String> weaknesses,
        LocalDateTime createdAt, String experienceLevel, int currentQuestionIndex,
        List<InterviewQuestion> questions, String assessmentMode, String assessmentNote
    ) {}
    public record InterviewCreateRequest(
        @NotBlank @Size(max = 120) String role,
        @NotBlank @Pattern(regexp = "Fresher|Entry-level|Mid-level") String experienceLevel,
        @NotBlank @Pattern(regexp = "Easy|Medium|Hard") String difficulty,
        @Min(3) @Max(15) int questionCount
    ) {}
    public record InterviewAnswerRequest(
        @NotNull Long questionId,
        @NotBlank @Size(max = 6000) String answer
    ) {}
    public record InterviewAnswerResult(InterviewQuestion evaluation, InterviewDetail interview, boolean completed) {}

    public record ResumeSummary(
        Long id, String fileName, String targetRole, LocalDateTime createdAt,
        int atsScore, int overallScore, String assessmentMode
    ) {}
    public record ResumeAnalysis(
        int atsScore, int overallScore, List<String> technicalSkills, List<String> softSkills,
        List<String> missingSkills, List<String> strengths, List<String> weaknesses,
        List<String> suggestions, List<String> recommendedTechnologies, List<String> recommendedRoles,
        List<String> keywords, List<String> projectSuggestions, String assessmentMode, String assessmentNote
    ) {}
    public record ResumeDetail(Long id, String fileName, String targetRole, LocalDateTime createdAt,
                               int atsScore, int overallScore, String assessmentMode, ResumeAnalysis analysis) {}
    public record ResumeImprovementRequest(
        @NotBlank @Pattern(regexp = "SUMMARY|PROJECT|EXPERIENCE|BULLET") String section,
        @NotBlank @Size(max = 5000) String originalText,
        @NotBlank @Size(max = 240) String goal
    ) {}
    public record ResumeImprovement(String original, String suggestion, String assessmentMode, String assessmentNote) {}

    public record Skill(String name, String category) {}
    public record JobRole(String id, String name, List<String> requiredSkills) {}
    public record JobMatchRequest(
        @NotBlank @Size(max = 120) String role,
        @NotNull @Size(max = 100) List<@NotBlank @Size(max = 64) String> skills
    ) {}
    public record JobMatch(String role, int matchPercentage, List<String> matchingSkills,
                           List<String> missingSkills, List<String> learningOrder) {}

    public record ScorePoint(LocalDateTime date, double score) {}
    public record InterviewAnalytics(double averageScore, String strongestTopic, String weakestTopic,
                                     double improvementTrend, List<ScorePoint> scoreHistory) {}
    public record Dashboard(
        Integer resumeAtsScore, long resumesAnalyzed, long interviewsCompleted,
        Double averageInterviewScore, String targetRole, List<String> skillGaps,
        List<String> recommendedSkills, List<InterviewSummary> recentInterviews, String assessmentMode
    ) {}
    public record StudyTopic(Long id, int week, String title, String description, boolean completed, int orderIndex) {}
    public record StudyPlan(String targetRole, int completionPercentage, List<StudyTopic> topics) {}
    public record StudyTopicUpdate(@NotNull Boolean completed) {}
}
