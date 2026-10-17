package com.fixflow.service;

import com.fixflow.dto.request.FeedbackRequest;
import com.fixflow.dto.response.FeedbackResponse;
import com.fixflow.entity.Diagnosis;
import com.fixflow.exception.DiagnosisNotFoundException;
import com.fixflow.repository.DiagnosisRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores user feedback only. It never trains or fine-tunes anything. */
@Service
public class FeedbackService {

    private static final int MAX_COMMENT_LENGTH = 500;

    private final DiagnosisRepository repository;
    private final SecretRedactionService redactionService;

    public FeedbackService(DiagnosisRepository repository, SecretRedactionService redactionService) {
        this.repository = repository;
        this.redactionService = redactionService;
    }

    @Transactional
    public FeedbackResponse submit(UUID id, FeedbackRequest request) {
        Diagnosis diagnosis = repository.findById(id).orElseThrow(() -> new DiagnosisNotFoundException(id));
        diagnosis.setFeedback(request.rating(), cleanComment(request.comment()));
        Diagnosis saved = repository.save(diagnosis);
        return new FeedbackResponse(id, saved.getFeedbackRating(), saved.getFeedbackComment());
    }

    private String cleanComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        String sanitized = redactionService.redact(comment.strip()).sanitizedText();
        return sanitized.length() <= MAX_COMMENT_LENGTH ? sanitized : sanitized.substring(0, MAX_COMMENT_LENGTH);
    }
}