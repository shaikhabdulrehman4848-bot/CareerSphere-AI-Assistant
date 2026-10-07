package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.DashboardService;
import com.careersphere.service.JobService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
public class CareerController {
    private final DashboardService dashboard;
    private final JobService jobs;
    public CareerController(DashboardService dashboard, JobService jobs) { this.dashboard = dashboard; this.jobs = jobs; }

    @GetMapping("/dashboard")
    public Dto.Dashboard dashboard() { return dashboard.get(); }

    @GetMapping("/skills")
    public List<Dto.Skill> skills() { return jobs.listSkills(); }

    @GetMapping("/job-roles")
    public List<Dto.JobRole> roles() { return jobs.listRoles(); }

    @PostMapping("/job-matches")
    public Dto.JobMatch match(@Valid @RequestBody Dto.JobMatchRequest request) { return jobs.match(request); }
}
