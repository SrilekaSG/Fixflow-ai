package com.fixflow.controller;

import com.fixflow.dto.request.FeedbackRequest;
import com.fixflow.dto.response.FeedbackResponse;
import com.fixflow.service.FeedbackService;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/diagnoses")
public class FeedbackController {

    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping("/{id}/feedback")
    public FeedbackResponse submit(@PathVariable UUID id, @Valid @RequestBody FeedbackRequest request) {
        return feedbackService.submit(id, request);
    }
}