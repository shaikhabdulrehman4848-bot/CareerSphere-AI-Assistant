package com.careersphere.controller;

import com.careersphere.dto.Dto;
import com.careersphere.service.InterviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/interviews")
public class InterviewController {
    private final InterviewService interviews;
    public InterviewController(InterviewService interviews) { this.interviews = interviews; }

    @GetMapping
    public List<Dto.InterviewSummary> list() { return interviews.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Dto.InterviewDetail create(@Valid @RequestBody Dto.InterviewCreateRequest request) { return interviews.create(request); }

    @GetMapping("/{interviewId}")
    public Dto.InterviewDetail get(@PathVariable long interviewId) { return interviews.get(interviewId); }

    @PostMapping("/{interviewId}/answer")
    public Dto.InterviewAnswerResult submit(@PathVariable long interviewId,
                                            @Valid @RequestBody Dto.InterviewAnswerRequest request) {
        return interviews.submit(interviewId, request);
    }
}
