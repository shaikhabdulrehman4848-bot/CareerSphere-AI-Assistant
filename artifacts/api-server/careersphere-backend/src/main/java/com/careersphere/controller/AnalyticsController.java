package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.InterviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnalyticsController {
    private final InterviewService interviews;
    public AnalyticsController(InterviewService interviews) { this.interviews = interviews; }
    @GetMapping("/analytics")
    public Dto.InterviewAnalytics analytics() { return interviews.analytics(); }
}
