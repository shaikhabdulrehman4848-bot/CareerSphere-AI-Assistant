package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.StudyPlanService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/study-plan")
public class StudyPlanController {
    private final StudyPlanService plans;
    public StudyPlanController(StudyPlanService plans) { this.plans = plans; }

    @GetMapping
    public Dto.StudyPlan get() { return plans.get(); }

    @PutMapping("/topics/{topicId}")
    public Dto.StudyTopic update(@PathVariable long topicId, @Valid @RequestBody Dto.StudyTopicUpdate request) {
        return plans.update(topicId, request);
    }
}
