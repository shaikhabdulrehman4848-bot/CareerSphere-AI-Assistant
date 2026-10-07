package com.careersphere.service;

import com.careersphere.ai.AiService;
import com.careersphere.dto.Dto;
import com.careersphere.entity.Entities.Resume;
import com.careersphere.entity.Entities.ResumeAnalysis;
import com.careersphere.entity.Entities.User;
import com.careersphere.exception.ApiException;
import com.careersphere.repository.ResumeRepository;
import java.io.IOException;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ResumeService {
    private static final long MAX_BYTES = 8L * 1024 * 1024;
    private final CurrentUserService currentUser;
    private final ResumeRepository resumes;
    private final AiService ai;

    public ResumeService(CurrentUserService currentUser, ResumeRepository resumes, AiService ai) {
        this.currentUser = currentUser;
        this.resumes = resumes;
        this.ai = ai;
    }

    @Transactional
    public Dto.ResumeDetail upload(MultipartFile file, String targetRole) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Choose a PDF resume to upload.");
        if (file.getSize() > MAX_BYTES) throw new ApiException(org.springframework.http.HttpStatus.PAYLOAD_TOO_LARGE, "This PDF is larger than the 8 MB upload limit.");
        String name = file.getOriginalFilename() == null ? "resume.pdf" : file.getOriginalFilename().replaceAll("[\\r\\n\\\\/]", "_");
        if (!name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) throw ApiException.badRequest("Only PDF files are supported.");
        if (targetRole == null || targetRole.isBlank()) throw ApiException.badRequest("Choose the target role for this assessment.");

        byte[] bytes;
        String extracted;
        try {
            bytes = file.getBytes();
            if (bytes.length < 5 || bytes[0] != '%' || bytes[1] != 'P' || bytes[2] != 'D' || bytes[3] != 'F' || bytes[4] != '-') {
                throw ApiException.badRequest("This file is not a valid PDF.");
            }
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) throw ApiException.badRequest("This PDF is password protected. Upload an unlocked copy.");
                extracted = new PDFTextStripper().getText(document).trim();
                if (document.getNumberOfPages() == 0 || extracted.length() < 30) {
                    throw ApiException.badRequest("We could not find readable text. Upload a text-based PDF rather than a scanned image.");
                }
            }
        } catch (ApiException exception) {
            throw exception;
        } catch (IOException | RuntimeException exception) {
            throw ApiException.badRequest("We could not read this PDF. Check that it is not damaged or password protected.");
        }

        User user = currentUser.requireUser();
        Resume resume = new Resume();
        resume.user = user;
        resume.fileName = name.length() > 255 ? name.substring(name.length() - 255) : name;
        resume.targetRole = targetRole.trim();
        resume.pdfContent = bytes;
        resume.extractedText = extracted.substring(0, Math.min(extracted.length(), 50000));
        AiService.Result<Dto.ResumeAnalysis> result = ai.analyzeResume(resume.extractedText, resume.targetRole);
        resume.analysis = toEntity(result.value());
        resume.analysis.resume = resume;
        resume = resumes.save(resume);
        return toDetail(resume);
    }

    @Transactional(readOnly = true)
    public List<Dto.ResumeSummary> list() {
        return resumes.findByUserIdOrderByCreatedAtDesc(currentUser.requireUser().id).stream().map(this::toSummary).toList();
    }

    @Transactional(readOnly = true)
    public Dto.ResumeDetail get(long id) { return toDetail(requireResume(id)); }

    @Transactional(readOnly = true)
    public Dto.ResumeAnalysis getAnalysis(long id) {
        Resume resume = requireResume(id);
        if (resume.analysis == null) throw ApiException.notFound("No assessment is available for this resume.");
        return toDto(resume.analysis);
    }

    public Dto.ResumeImprovement improve(Dto.ResumeImprovementRequest request) {
        AiService.Result<String> result = ai.improveSection(request.section(), request.originalText(), request.goal());
        return new Dto.ResumeImprovement(request.originalText(), result.value(), result.mode(), result.note());
    }

    private Resume requireResume(long id) {
        return resumes.findByIdAndUserId(id, currentUser.requireUser().id)
            .orElseThrow(() -> ApiException.notFound("Resume not found."));
    }

    private ResumeAnalysis toEntity(Dto.ResumeAnalysis dto) {
        ResumeAnalysis entity = new ResumeAnalysis();
        entity.atsScore = dto.atsScore();
        entity.overallScore = dto.overallScore();
        entity.technicalSkills = dto.technicalSkills();
        entity.softSkills = dto.softSkills();
        entity.missingSkills = dto.missingSkills();
        entity.strengths = dto.strengths();
        entity.weaknesses = dto.weaknesses();
        entity.suggestions = dto.suggestions();
        entity.recommendedTechnologies = dto.recommendedTechnologies();
        entity.recommendedRoles = dto.recommendedRoles();
        entity.keywords = dto.keywords();
        entity.projectSuggestions = dto.projectSuggestions();
        entity.assessmentMode = dto.assessmentMode();
        entity.assessmentNote = dto.assessmentNote();
        return entity;
    }

    private Dto.ResumeSummary toSummary(Resume resume) {
        int ats = resume.analysis == null ? 0 : resume.analysis.atsScore;
        int overall = resume.analysis == null ? 0 : resume.analysis.overallScore;
        String mode = resume.analysis == null ? "DEMO" : resume.analysis.assessmentMode;
        return new Dto.ResumeSummary(resume.id, resume.fileName, resume.targetRole, resume.createdAt, ats, overall, mode);
    }

    private Dto.ResumeDetail toDetail(Resume resume) {
        Dto.ResumeSummary summary = toSummary(resume);
        Dto.ResumeAnalysis analysis = resume.analysis == null ? null : toDto(resume.analysis);
        return new Dto.ResumeDetail(summary.id(), summary.fileName(), summary.targetRole(), summary.createdAt(),
            summary.atsScore(), summary.overallScore(), summary.assessmentMode(), analysis);
    }

    private Dto.ResumeAnalysis toDto(ResumeAnalysis analysis) {
        return new Dto.ResumeAnalysis(analysis.atsScore, analysis.overallScore, analysis.technicalSkills,
            analysis.softSkills, analysis.missingSkills, analysis.strengths, analysis.weaknesses,
            analysis.suggestions, analysis.recommendedTechnologies, analysis.recommendedRoles,
            analysis.keywords, analysis.projectSuggestions, analysis.assessmentMode, analysis.assessmentNote);
    }
}
