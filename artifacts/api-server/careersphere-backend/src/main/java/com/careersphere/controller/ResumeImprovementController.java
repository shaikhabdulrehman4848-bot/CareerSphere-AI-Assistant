package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.ResumeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
public class ResumeImprovementController {
    private final ResumeService resumes;
    public ResumeImprovementController(ResumeService resumes) { this.resumes = resumes; }

    @PostMapping("/resume-improvements")
    public Dto.ResumeImprovement improve(@Valid @RequestBody Dto.ResumeImprovementRequest request) {
        return resumes.improve(request);
    }
}
