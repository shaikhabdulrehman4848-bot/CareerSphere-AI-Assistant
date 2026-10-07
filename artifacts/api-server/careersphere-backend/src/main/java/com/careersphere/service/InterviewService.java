package com.careersphere.service;

import com.careersphere.ai.AiService;
import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.Interview;
import com.careersphere.entity.Entities.InterviewAnswer;
import com.careersphere.entity.Entities.InterviewQuestion;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.InterviewQuestionRepository;
import com.careersphere.repository.InterviewRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {
    private final CurrentUserService currentUser;
    private final InterviewRepository interviews;
    private final InterviewQuestionRepository questions;
    private final AiService ai;

    public InterviewService(CurrentUserService currentUser, InterviewRepository interviews,
                            InterviewQuestionRepository questions, AiService ai) {
        this.currentUser = currentUser;
        this.interviews = interviews;
        this.questions = questions;
        this.ai = ai;
    }

    @Transactional
    public Dto.InterviewDetail create(Dto.InterviewCreateRequest input) {
        User user = currentUser.requireUser();
        AiService.Result<List<AiService.QuestionSeed>> generated =
            ai.generateQuestions(input.role(), input.experienceLevel(), input.difficulty(), input.questionCount());
        Interview interview = new Interview();
        interview.user = user;
        interview.role = input.role().trim();
        interview.experienceLevel = input.experienceLevel();
        interview.difficulty = input.difficulty();
        interview.questionCount = input.questionCount();
        interview.assessmentMode = generated.mode();
        interview.assessmentNote = generated.note();
        int number = 1;
        for (AiService.QuestionSeed seed : generated.value()) {
            InterviewQuestion question = new InterviewQuestion();
            question.interview = interview;
            question.questionNumber = number++;
            question.category = seed.category();
            question.prompt = seed.prompt();
            interview.questions.add(question);
        }
        return toDetail(interviews.save(interview));
    }

    @Transactional(readOnly = true)
    public List<Dto.InterviewSummary> list() {
        return interviews.findByUserIdOrderByCreatedAtDesc(currentUser.requireUser().id)
            .stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Dto.InterviewDetail get(long id) { return toDetail(requireInterview(id)); }

    @Transactional
    public Dto.InterviewAnswerResult submit(long id, Dto.InterviewAnswerRequest request) {
        Interview interview = requireInterview(id);
        if ("COMPLETED".equals(interview.status)) throw ApiException.conflict("This interview is already complete.");
        InterviewQuestion next = interview.questions.stream().filter(question -> question.answer == null).findFirst()
            .orElseThrow(() -> ApiException.conflict("This interview is already complete."));
        if (!next.id.equals(request.questionId())) throw ApiException.badRequest("Answer the current question before moving ahead.");
        InterviewQuestion question = questions.findByIdAndInterviewId(request.questionId(), interview.id)
            .orElseThrow(() -> ApiException.notFound("Interview question not found."));

        AiService.Result<AiService.Evaluation> evaluation = ai.evaluateAnswer(
            interview.role, question.category, question.prompt, request.answer());
        AiService.Evaluation value = evaluation.value();
        InterviewAnswer answer = new InterviewAnswer();
        answer.question = question;
        answer.answerText = request.answer().trim();
        answer.score = value.score();
        answer.technicalCorrectness = value.technicalCorrectness();
        answer.completeness = value.completeness();
        answer.clarity = value.clarity();
        answer.relevance = value.relevance();
        answer.communication = value.communication();
        answer.feedback = value.feedback();
        answer.assessmentMode = evaluation.mode();
        question.answer = answer;
        questions.save(question);

        long answered = interview.questions.stream().filter(item -> item.answer != null).count();
        if (answered >= interview.questionCount) {
            interview.status = "COMPLETED";
            interview.overallScore = interview.questions.stream().mapToDouble(item -> item.answer.score).average().orElse(0);
            interview.assessmentMode = interview.questions.stream().allMatch(item -> "GEMINI".equals(item.answer.assessmentMode)) ? "GEMINI" : "DEMO";
            interview.assessmentNote = interview.questions.stream().filter(item -> item.answer != null)
                .map(item -> item.answer.assessmentMode).anyMatch(mode -> "DEMO".equals(mode))
                ? "At least one answer used the marked demo evaluator." : null;
            updateTopicSummary(interview);
        }
        Interview saved = interviews.save(interview);
        return new Dto.InterviewAnswerResult(toQuestion(question), toDetail(saved), "COMPLETED".equals(saved.status));
    }

    @Transactional(readOnly = true)
    public Dto.InterviewAnalytics analytics() {
        List<Interview> completed = interviews.findByUserIdOrderByCreatedAtDesc(currentUser.requireUser().id).stream()
            .filter(interview -> "COMPLETED".equals(interview.status)).toList();
        if (completed.isEmpty()) return new Dto.InterviewAnalytics(0, null, null, 0, List.of());
        double average = completed.stream().mapToDouble(interview -> interview.overallScore == null ? 0 : interview.overallScore).average().orElse(0);
        Map<String, List<Double>> topicScores = new HashMap<>();
        for (Interview interview : completed) for (InterviewQuestion question : interview.questions) {
            if (question.answer != null) topicScores.computeIfAbsent(question.category, key -> new ArrayList<>()).add(question.answer.score);
        }
        Comparator<Map.Entry<String, List<Double>>> comparator = Comparator.comparingDouble(entry ->
            entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0));
        String weakest = topicScores.entrySet().stream().min(comparator).map(Map.Entry::getKey).orElse(null);
        String strongest = topicScores.entrySet().stream().max(comparator).map(Map.Entry::getKey).orElse(null);
        double trend = improvementTrend(completed);
        List<Dto.ScorePoint> history = new ArrayList<>(completed.stream().limit(12).map(interview ->
            new Dto.ScorePoint(interview.createdAt, interview.overallScore == null ? 0 : interview.overallScore)).toList());
        java.util.Collections.reverse(history);
        return new Dto.InterviewAnalytics(round(average), strongest, weakest, round(trend), history);
    }

    public Dto.InterviewSummary toSummary(Interview interview) {
        int answered = (int) interview.questions.stream().filter(question -> question.answer != null).count();
        return new Dto.InterviewSummary(interview.id, interview.role, interview.difficulty, interview.questionCount,
            answered, interview.status, interview.overallScore, interview.strengths, interview.weaknesses, interview.createdAt);
    }

    public Dto.InterviewDetail toDetail(Interview interview) {
        List<Dto.InterviewQuestion> items = interview.questions.stream().sorted(Comparator.comparingInt(question -> question.questionNumber))
            .map(this::toQuestion).toList();
        int answered = (int) interview.questions.stream().filter(question -> question.answer != null).count();
        return new Dto.InterviewDetail(interview.id, interview.role, interview.difficulty, interview.questionCount,
            answered, interview.status, interview.overallScore, interview.strengths, interview.weaknesses,
            interview.createdAt, interview.experienceLevel, answered, items, interview.assessmentMode, interview.assessmentNote);
    }

    private Dto.InterviewQuestion toQuestion(InterviewQuestion question) {
        InterviewAnswer answer = question.answer;
        return new Dto.InterviewQuestion(question.id, question.questionNumber, question.category, question.prompt,
            answer != null, answer == null ? null : answer.answerText, answer == null ? null : answer.score,
            answer == null ? null : answer.technicalCorrectness, answer == null ? null : answer.completeness,
            answer == null ? null : answer.clarity, answer == null ? null : answer.relevance,
            answer == null ? null : answer.communication,
            answer == null ? null : answer.feedback);
    }

    private Interview requireInterview(long id) {
        return interviews.findByIdAndUserId(id, currentUser.requireUser().id)
            .orElseThrow(() -> ApiException.notFound("Interview not found."));
    }

    private void updateTopicSummary(Interview interview) {
        Map<String, List<Double>> categories = new HashMap<>();
        for (InterviewQuestion question : interview.questions) if (question.answer != null) {
            categories.computeIfAbsent(question.category, key -> new ArrayList<>()).add(question.answer.score);
        }
        var byScore = categories.entrySet().stream().sorted(Comparator.comparingDouble(entry ->
            entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0))).toList();
        interview.weaknesses = byScore.stream().filter(entry ->
            entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) < 6.5)
            .limit(3).map(Map.Entry::getKey).toList();
        interview.strengths = byScore.stream().filter(entry ->
            entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0) >= 7.5)
            .sorted(Comparator.comparingDouble((Map.Entry<String, List<Double>> entry) ->
                entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0)).reversed())
            .limit(3).map(Map.Entry::getKey).toList();
    }

    private double improvementTrend(List<Interview> completed) {
        if (completed.size() < 2) return 0;
        int recentCount = Math.min(3, completed.size());
        double recent = completed.subList(0, recentCount).stream().mapToDouble(item -> item.overallScore == null ? 0 : item.overallScore).average().orElse(0);
        int olderStart = recentCount;
        if (olderStart >= completed.size()) return 0;
        int olderEnd = Math.min(completed.size(), olderStart + 3);
        double previous = completed.subList(olderStart, olderEnd).stream().mapToDouble(item -> item.overallScore == null ? 0 : item.overallScore).average().orElse(0);
        return recent - previous;
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
