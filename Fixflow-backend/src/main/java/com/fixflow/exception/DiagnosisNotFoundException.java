package com.fixflow.exception;

import java.util.UUID;

public class DiagnosisNotFoundException extends RuntimeException {

    public DiagnosisNotFoundException(UUID id) {
        super("Diagnosis " + id + " was not found.");
    }
}