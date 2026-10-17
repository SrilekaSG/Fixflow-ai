package com.fixflow.service;

import com.fixflow.entity.Severity;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Connects parser key patterns to knowledge articles, severity estimates, and priority. */
public final class ErrorSignatures {

    private ErrorSignatures() {
    }

    /** Most specific signature first. The first matching one becomes the primary diagnosis. */
    public static final List<String> PRIORITY = List.of(
            "POSTGRES_AUTH_FAILED",
            "UNKNOWN_DATABASE",
            "CONNECTION_REFUSED",
            "DATASOURCE_NOT_CONFIGURED",
            "UNRESOLVED_PLACEHOLDER",
            "NO_QUALIFYING_BEAN",
            "PORT_IN_USE",
            "CLASS_NOT_FOUND",
            "MAVEN_BUILD_FAILURE",
            "NULL_POINTER",
            "JSON_OR_VALIDATION",
            "HTTP_404",
            "HTTP_500",
            "BEAN_CREATION_FAILED");

    public static final Map<String, String> ARTICLE_BY_PATTERN = Map.ofEntries(
            Map.entry("POSTGRES_AUTH_FAILED", "pg-auth-failed"),
            Map.entry("UNKNOWN_DATABASE", "db-unknown-database"),
            Map.entry("CONNECTION_REFUSED", "db-connection-refused"),
            Map.entry("DATASOURCE_NOT_CONFIGURED", "spring-datasource-config"),
            Map.entry("UNRESOLVED_PLACEHOLDER", "missing-env-or-placeholder"),
            Map.entry("NO_QUALIFYING_BEAN", "spring-no-qualifying-bean"),
            Map.entry("PORT_IN_USE", "port-already-in-use"),
            Map.entry("CLASS_NOT_FOUND", "class-not-found"),
            Map.entry("MAVEN_BUILD_FAILURE", "maven-build-failure"),
            Map.entry("NULL_POINTER", "java-null-pointer"),
            Map.entry("JSON_OR_VALIDATION", "invalid-json-validation"),
            Map.entry("HTTP_404", "http-404-not-found"),
            Map.entry("HTTP_500", "http-500-server-error"),
            Map.entry("BEAN_CREATION_FAILED", "spring-bean-creation-failure"));

    /** Rough estimates only: how badly the problem usually affects the application. */
    public static final Map<String, Severity> SEVERITY_BY_PATTERN = Map.ofEntries(
            Map.entry("POSTGRES_AUTH_FAILED", Severity.HIGH),
            Map.entry("UNKNOWN_DATABASE", Severity.HIGH),
            Map.entry("CONNECTION_REFUSED", Severity.HIGH),
            Map.entry("DATASOURCE_NOT_CONFIGURED", Severity.HIGH),
            Map.entry("UNRESOLVED_PLACEHOLDER", Severity.HIGH),
            Map.entry("NO_QUALIFYING_BEAN", Severity.HIGH),
            Map.entry("PORT_IN_USE", Severity.MEDIUM),
            Map.entry("CLASS_NOT_FOUND", Severity.HIGH),
            Map.entry("MAVEN_BUILD_FAILURE", Severity.MEDIUM),
            Map.entry("NULL_POINTER", Severity.MEDIUM),
            Map.entry("JSON_OR_VALIDATION", Severity.LOW),
            Map.entry("HTTP_404", Severity.LOW),
            Map.entry("HTTP_500", Severity.MEDIUM),
            Map.entry("BEAN_CREATION_FAILED", Severity.HIGH));

    /** Signatures whose error text is very distinctive. */
    public static final Set<String> SPECIFIC = Set.of(
            "POSTGRES_AUTH_FAILED",
            "UNKNOWN_DATABASE",
            "DATASOURCE_NOT_CONFIGURED",
            "UNRESOLVED_PLACEHOLDER",
            "NO_QUALIFYING_BEAN",
            "PORT_IN_USE",
            "CLASS_NOT_FOUND");

    public static List<String> orderedPatterns(Collection<String> found) {
        return PRIORITY.stream().filter(found::contains).toList();
    }
}