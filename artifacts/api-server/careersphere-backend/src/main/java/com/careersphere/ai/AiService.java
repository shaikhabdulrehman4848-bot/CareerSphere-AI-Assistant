package com.careersphere.ai;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class AiService {
    public record Result<T>(T value, String mode, String note) {}
    public record QuestionSeed(String category, String prompt) {}
    public record Evaluation(double score, double technicalCorrectness, double completeness,
                             double clarity, double relevance, double communication, String feedback) {}

    private final GeminiAiProvider provider;
    private final ObjectMapper mapper;

    public AiService(GeminiAiProvider provider, ObjectMapper mapper) {
        this.provider = provider;
        this.mapper = mapper;
    }

    public Result<Dto.ResumeAnalysis> analyzeResume(String text, String role) {
        if (provider.isConfigured()) {
            try {
                String prompt = """
                    Assess this resume for the target role: %s.
                    This is an informational AI assessment, not a real employer ATS score.
                    Return only JSON with keys atsScore (0-100 integer), overallScore (0-100 integer),
                    technicalSkills, softSkills, missingSkills, strengths, weaknesses, suggestions,
                    recommendedTechnologies, recommendedRoles, keywords, projectSuggestions (all arrays
                    of concise strings). Be constructive, specific, and do not invent experience.
                    Resume text:
                    %s
                    """.formatted(role, text.substring(0, Math.min(text.length(), 14000)));
                JsonNode json = mapper.readTree(provider.generateJson(prompt));
                Dto.ResumeAnalysis assessment = new Dto.ResumeAnalysis(
                    clamp(json.path("atsScore").asInt(0), 0, 100),
                    clamp(json.path("overallScore").asInt(0), 0, 100),
                    strings(json, "technicalSkills"), strings(json, "softSkills"), strings(json, "missingSkills"),
                    strings(json, "strengths"), strings(json, "weaknesses"), strings(json, "suggestions"),
                    strings(json, "recommendedTechnologies"), strings(json, "recommendedRoles"),
                    strings(json, "keywords"), strings(json, "projectSuggestions"), "GEMINI", null
                );
                return new Result<>(assessment, "GEMINI", null);
            } catch (Exception ignored) {
                return demoAssessment(text, role, "Live AI was unavailable for this assessment, so a clearly marked demo assessment is shown.");
            }
        }
        return demoAssessment(text, role, "Demo assessment: configure GEMINI_API_KEY on the server to enable live AI feedback.");
    }

    public Result<String> improveSection(String section, String original, String goal) {
        if (provider.isConfigured()) {
            try {
                JsonNode json = mapper.readTree(provider.generateJson("""
                    Improve the user's resume %s text for this goal: %s.
                    Keep claims truthful, do not add unsupported facts, preserve core meaning,
                    and return only JSON { "suggestion": "..." }.
                    Original:
                    %s
                    """.formatted(section.toLowerCase(Locale.ROOT), goal, original)));
                String suggestion = json.path("suggestion").asText("").trim();
                if (!suggestion.isBlank()) return new Result<>(suggestion, "GEMINI", null);
            } catch (Exception ignored) {
                return new Result<>(demoImprovement(original, goal), "DEMO",
                    "Live AI was unavailable; review this demo suggestion before using it.");
            }
        }
        return new Result<>(demoImprovement(original, goal), "DEMO",
            "Demo suggestion: configure GEMINI_API_KEY on the server for live AI rewriting.");
    }

    public Result<List<QuestionSeed>> generateQuestions(String role, String experience, String difficulty, int count) {
        if (provider.isConfigured()) {
            try {
                JsonNode json = mapper.readTree(provider.generateJson("""
                    Create exactly %d concise mock interview questions for a %s applying as a %s at %s difficulty.
                    Mix role-specific technical and behavioral questions. Return only JSON:
                    { "questions": [ { "category": "Core Java", "prompt": "..." } ] }.
                    """.formatted(count, experience, role, difficulty)));
                List<QuestionSeed> generated = new ArrayList<>();
                JsonNode items = json.path("questions");
                if (items.isArray()) for (JsonNode item : items) {
                    String category = item.path("category").asText("Backend development");
                    String prompt = item.path("prompt").asText("").trim();
                    if (!prompt.isBlank()) generated.add(new QuestionSeed(category, prompt));
                }
                if (generated.size() >= count) return new Result<>(generated.subList(0, count), "GEMINI", null);
            } catch (Exception ignored) {
                return new Result<>(demoQuestions(count), "DEMO",
                    "Live AI was unavailable; this session is using the built-in demo question set.");
            }
        }
        return new Result<>(demoQuestions(count), "DEMO",
            "Demo interview: configure GEMINI_API_KEY on the server for generated questions and feedback.");
    }

    public Result<Evaluation> evaluateAnswer(String role, String category, String question, String answer) {
        if (provider.isConfigured()) {
            try {
                JsonNode json = mapper.readTree(provider.generateJson("""
                    Evaluate this mock interview answer for a %s role.
                    Question category: %s. Question: %s. Candidate answer: %s.
                    Return JSON with technicalCorrectness, completeness, clarity, relevance, communication
                    (each 0-10), overall score (0-10), and concise constructive feedback.
                    """.formatted(role, category, question, answer.substring(0, Math.min(answer.length(), 5000)))));
                Evaluation evaluation = new Evaluation(
                    clamp(json.path("score").asDouble(json.path("overallScore").asDouble(0)), 0, 10),
                    clamp(json.path("technicalCorrectness").asDouble(0), 0, 10),
                    clamp(json.path("completeness").asDouble(0), 0, 10),
                    clamp(json.path("clarity").asDouble(0), 0, 10),
                    clamp(json.path("relevance").asDouble(0), 0, 10),
                    clamp(json.path("communication").asDouble(0), 0, 10),
                    json.path("feedback").asText("Review your answer for technical detail and clarity.")
                );
                return new Result<>(evaluation, "GEMINI", null);
            } catch (Exception ignored) {
                return new Result<>(demoEvaluation(answer), "DEMO",
                    "Live AI was unavailable; this answer received a clearly marked demo evaluation.");
            }
        }
        return new Result<>(demoEvaluation(answer), "DEMO",
            "Demo evaluation: configure GEMINI_API_KEY on the server for AI-generated feedback.");
    }

    private Result<Dto.ResumeAnalysis> demoAssessment(String text, String role, String note) {
        String lower = text.toLowerCase(Locale.ROOT);
        List<String> technical = new ArrayList<>();
        List<String> catalog = List.of("Java", "Spring Boot", "Spring Security", "REST APIs", "SQL", "MySQL",
            "JPA & Hibernate", "Git", "Maven", "Docker", "AWS", "OOP", "Collections", "Multithreading",
            "JUnit", "Microservices");
        for (String skill : catalog) if (lower.contains(skill.toLowerCase(Locale.ROOT).replace(" & ", " "))) technical.add(skill);
        List<String> roleSkills = roleSkills(role);
        List<String> missing = roleSkills.stream().filter(skill -> technical.stream().noneMatch(found ->
            found.equalsIgnoreCase(skill) || (skill.equals("Spring Security") && lower.contains("security")))).toList();
        int ats = clamp(42 + technical.size() * 4 + (lower.contains("project") ? 5 : 0) + (lower.contains("education") ? 3 : 0), 24, 94);
        List<String> strengths = new ArrayList<>();
        technical.stream().limit(5).forEach(skill -> strengths.add(skill + " appears in your resume"));
        if (lower.contains("project")) strengths.add("Projects provide practical experience to discuss.");
        if (strengths.isEmpty()) strengths.add("Your resume text was extracted and is ready for a more detailed revision.");
        List<String> suggestions = new ArrayList<>(List.of(
            "Add measurable outcomes to project bullets, such as response time, test coverage, or user impact.",
            "Place role-relevant technical skills near the top and use consistent section headings.",
            "Tailor the summary and project language to the selected target role."
        ));
        if (!missing.isEmpty()) suggestions.add("Show relevant practice with " + String.join(", ", missing.stream().limit(3).toList()) + " where accurate.");
        var value = new Dto.ResumeAnalysis(ats, Math.max(0, ats - 3), technical,
            detectSoftSkills(lower), missing, strengths,
            missing.isEmpty() ? List.of("Add more quantified achievements and project context.") : List.of("Some common " + role + " skills were not found in the resume text."),
            suggestions, missing, List.of("Java Backend Developer", "Software Engineer"),
            roleSkills.stream().limit(6).toList(),
            List.of("Improved API response time by X%", "Added automated tests and measured coverage"),
            "DEMO", note);
        return new Result<>(value, "DEMO", note);
    }

    private Dto.ResumeAnalysis emptyAnalysis() {
        return new Dto.ResumeAnalysis(0, 0, List.of(), List.of(), List.of(), List.of(), List.of(),
            List.of(), List.of(), List.of(), List.of(), List.of(), "DEMO", null);
    }

    private List<String> detectSoftSkills(String text) {
        List<String> words = List.of("communication", "leadership", "collaboration", "problem solving", "teamwork");
        return words.stream().filter(text::contains).map(word -> word.equals("problem solving") ? "Problem solving" : title(word)).toList();
    }

    private String title(String word) { return Character.toUpperCase(word.charAt(0)) + word.substring(1); }

    private List<String> roleSkills(String role) {
        String key = role.toLowerCase(Locale.ROOT);
        if (key.contains("spring")) return List.of("Java", "Spring Boot", "REST APIs", "Spring Security", "SQL", "JPA & Hibernate", "Git");
        if (key.contains("software")) return List.of("Java", "OOP", "Data Structures", "SQL", "REST APIs", "Git", "JUnit");
        return List.of("Java", "Spring Boot", "REST APIs", "SQL", "MySQL", "JPA & Hibernate", "Git");
    }

    private List<QuestionSeed> demoQuestions(int count) {
        List<QuestionSeed> bank = List.of(
            new QuestionSeed("Core Java", "How does the JVM execute a Java program, and what happens during class loading?"),
            new QuestionSeed("OOP", "Explain encapsulation, inheritance, polymorphism, and abstraction with examples."),
            new QuestionSeed("Collections", "How do HashMap and ConcurrentHashMap differ, and how does a HashMap handle collisions?"),
            new QuestionSeed("Exception Handling", "When would you use a checked exception versus an unchecked exception?"),
            new QuestionSeed("Multithreading", "What is the difference between a process, a thread, and a synchronized block?"),
            new QuestionSeed("SQL", "How would you find duplicate rows in a table and then remove them safely?"),
            new QuestionSeed("DBMS", "Explain database normalization and when you would add an index."),
            new QuestionSeed("Spring Boot", "What does dependency injection solve, and how does Spring manage a bean's lifecycle?"),
            new QuestionSeed("REST APIs", "How do you choose between POST, PUT, and PATCH for a REST API?"),
            new QuestionSeed("Backend development", "How would you investigate an API endpoint that became slow after a release?"),
            new QuestionSeed("Projects", "Describe a technical tradeoff you made in a project and what you would change now."),
            new QuestionSeed("Behavioral", "Tell me about a time you received difficult feedback and how you acted on it."),
            new QuestionSeed("Spring Security", "How would you protect a stateless REST API using JWT authentication?"),
            new QuestionSeed("Testing", "What would you unit test and integration test in a Spring service?")
        );
        List<QuestionSeed> selected = new ArrayList<>();
        for (int index = 0; index < count; index++) selected.add(bank.get(index % bank.size()));
        return selected;
    }

    private String demoImprovement(String original, String goal) {
        String clean = original.replaceAll("\\s+", " ").trim();
        String first = clean.isEmpty() ? "" : Character.toUpperCase(clean.charAt(0)) + clean.substring(1);
        if (first.length() > 1 && !first.matches(".*[.!?]$")) first += ".";
        return first + " Emphasize the technical approach, your contribution, and a measurable outcome where you can verify one. Goal: " + goal.trim() + ".";
    }

    private Evaluation demoEvaluation(String answer) {
        int words = answer.trim().split("\\s+").length;
        double coverage = clamp(4.5 + Math.min(words, 110) / 32.0, 4.5, 8.0);
        return new Evaluation(coverage, coverage, Math.min(8.0, coverage - 0.2),
            Math.min(8.0, coverage + 0.1), Math.min(8.0, coverage - 0.1), Math.min(8.0, coverage),
            words < 22
                ? "Add a little more detail: explain the key idea, give a concrete example, and connect it to the role."
                : "Your answer has a useful foundation. Add a concrete example and explain the tradeoffs or impact to make it stronger.");
    }

    private List<String> strings(JsonNode object, String field) {
        List<String> values = new ArrayList<>();
        JsonNode list = object.path(field);
        if (list.isArray()) for (JsonNode item : list) {
            String value = item.asText("").trim();
            if (!value.isBlank()) values.add(value);
        }
        return values;
    }

    private int clamp(int value, int minimum, int maximum) { return Math.max(minimum, Math.min(maximum, value)); }
    private double clamp(double value, double minimum, double maximum) { return Math.max(minimum, Math.min(maximum, value)); }
}
