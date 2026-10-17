package com.fixflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fixflow.dto.response.DiagnosisDetailResponse;
import com.fixflow.dto.response.DiagnosisResponse;
import com.fixflow.dto.response.DiagnosisSummaryResponse;
import com.fixflow.dto.response.PageResponse;
import com.fixflow.dto.response.StatsResponse;
import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.Diagnosis;
import com.fixflow.entity.FeedbackRating;
import com.fixflow.exception.DiagnosisNotFoundException;
import com.fixflow.repository.DiagnosisRepository;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistoryService {

    static final int MAX_PAGE_SIZE = 50;

    private final DiagnosisRepository repository;
    private final ObjectMapper objectMapper;

    public HistoryService(DiagnosisRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public PageResponse<DiagnosisSummaryResponse> list(int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PageResponse.from(repository.findAll(pageable).map(DiagnosisSummaryResponse::from));
    }

    @Transactional(readOnly = true)
    public DiagnosisDetailResponse get(UUID id) {
        Diagnosis diagnosis = repository.findById(id).orElseThrow(() -> new DiagnosisNotFoundException(id));
        return new DiagnosisDetailResponse(
                diagnosis.getId(),
                diagnosis.getCreatedAt(),
                diagnosis.getUpdatedAt(),
                diagnosis.getTitle(),
                diagnosis.getFileName(),
                diagnosis.getSanitizedInput(),
                diagnosis.getRedactionCount(),
                diagnosis.getStatus(),
                diagnosis.getAnalysisMode(),
                diagnosis.getProviderName(),
                diagnosis.getModelName(),
                diagnosis.getFeedbackRating(),
                diagnosis.getFeedbackComment(),
                readDiagnosis(diagnosis));
    }

    @Transactional
    public void delete(UUID id) {
        if (!repository.existsById(id)) {
            throw new DiagnosisNotFoundException(id);
        }
        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        return new StatsResponse(
                repository.count(),
                repository.countByFeedbackRating(FeedbackRating.HELPFUL),
                repository.countByFeedbackRating(FeedbackRating.NOT_HELPFUL),
                repository.countByAnalysisMode(AnalysisMode.AI),
                repository.countByAnalysisMode(AnalysisMode.RULE_BASED));
    }

    private DiagnosisResponse readDiagnosis(Diagnosis diagnosis) {
        try {
            return objectMapper.readValue(diagnosis.getDiagnosisJson(), DiagnosisResponse.class)
                    .withDiagnosisId(diagnosis.getId());
        } catch (JsonProcessingException | IllegalArgumentException e) {
            throw new IllegalStateException("Stored diagnosis result could not be read", e);
        }
    }
}