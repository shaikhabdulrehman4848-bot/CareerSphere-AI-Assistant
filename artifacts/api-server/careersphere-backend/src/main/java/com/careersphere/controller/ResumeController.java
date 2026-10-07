package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.ResumeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/resumes")
public class ResumeController {
    private final ResumeService resumes;
    public ResumeController(ResumeService resumes) { this.resumes = resumes; }

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.ResumeDetail upload(@RequestPart("file") MultipartFile file, @RequestParam("targetRole") String targetRole) {
        return resumes.upload(file, targetRole);
    }

    @GetMapping
    public List<Dto.ResumeSummary> list() { return resumes.list(); }

    @GetMapping("/{resumeId}")
    public Dto.ResumeDetail get(@PathVariable long resumeId) { return resumes.get(resumeId); }

    @GetMapping("/{resumeId}/analysis")
    public Dto.ResumeAnalysis analysis(@PathVariable long resumeId) { return resumes.getAnalysis(resumeId); }
}
