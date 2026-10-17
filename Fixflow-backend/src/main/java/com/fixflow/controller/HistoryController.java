package com.fixflow.controller;

import com.fixflow.dto.response.DiagnosisDetailResponse;
import com.fixflow.dto.response.DiagnosisSummaryResponse;
import com.fixflow.dto.response.PageResponse;
import com.fixflow.dto.response.StatsResponse;
import com.fixflow.service.HistoryService;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class HistoryController {

    private final HistoryService historyService;

    public HistoryController(HistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/diagnoses")
    public PageResponse<DiagnosisSummaryResponse> list(@RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "10") int size) {
        return historyService.list(page, size);
    }

    @GetMapping("/diagnoses/{id}")
    public DiagnosisDetailResponse get(@PathVariable UUID id) {
        return historyService.get(id);
    }

    @DeleteMapping("/diagnoses/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        historyService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/stats")
    public StatsResponse stats() {
        return historyService.stats();
    }
}
