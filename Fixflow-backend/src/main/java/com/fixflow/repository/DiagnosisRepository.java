package com.fixflow.repository;

import com.fixflow.entity.AnalysisMode;
import com.fixflow.entity.Diagnosis;
import com.fixflow.entity.FeedbackRating;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, UUID> {

    long countByFeedbackRating(FeedbackRating rating);

    long countByAnalysisMode(AnalysisMode mode);
}