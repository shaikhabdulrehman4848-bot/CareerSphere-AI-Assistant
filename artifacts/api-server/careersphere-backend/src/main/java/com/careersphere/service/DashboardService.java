package com.careersphere.service;

import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.Interview;
import com.careersphere.entity.Entities.Resume;
import com.careersphere.entity.Entities.User;
import com.careersphere.repository.InterviewRepository;
import com.careersphere.repository.ResumeRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final CurrentUserService currentUser;
    private final ResumeRepository resumes;
    private final InterviewRepository interviews;
    private final InterviewService interviewService;
    public DashboardService(CurrentUserService currentUser, ResumeRepository resumes,
                            InterviewRepository interviews, InterviewService interviewService) {
        this.currentUser = currentUser;
        this.resumes = resumes;
        this.interviews = interviews;
        this.interviewService = interviewService;
    }

    @Transactional(readOnly = true)
    public Dto.Dashboard get() {
        User user = currentUser.requireUser();
        List<Resume> resumeList = resumes.findByUserIdOrderByCreatedAtDesc(user.id);
        List<Interview> sessions = interviews.findByUserIdOrderByCreatedAtDesc(user.id);
        List<Interview> completed = sessions.stream().filter(interview -> "COMPLETED".equals(interview.status)).toList();
        Double average = completed.isEmpty() ? null : round(completed.stream()
            .mapToDouble(interview -> interview.overallScore == null ? 0 : interview.overallScore).average().orElse(0));
        Resume latest = resumeList.isEmpty() ? null : resumeList.get(0);
        List<String> gaps = latest == null || latest.analysis == null ? List.of() : latest.analysis.missingSkills;
        List<String> recommended = latest == null || latest.analysis == null ? List.of("Spring Security", "Docker", "AWS")
            : latest.analysis.recommendedTechnologies.stream().limit(4).toList();
        List<Dto.InterviewSummary> recent = sessions.stream().limit(5).map(interviewService::toSummary).toList();
        String targetRole = user.targetRole != null && !user.targetRole.isBlank() ? user.targetRole
            : latest == null ? null : latest.targetRole;
        return new Dto.Dashboard(
            latest == null || latest.analysis == null ? null : latest.analysis.atsScore,
            resumeList.size(), completed.size(), average, targetRole, gaps, recommended, recent,
            latest == null || latest.analysis == null ? "DEMO" : latest.analysis.assessmentMode
        );
    }

    private double round(double value) { return Math.round(value * 100.0) / 100.0; }
}
