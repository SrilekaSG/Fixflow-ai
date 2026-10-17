package com.fixflow.ai;

import com.fixflow.entity.ErrorCategory;
import com.fixflow.entity.KnowledgeArticle;

public final class AiTestData {

    public static final String PG_AUTH_LOG =
            "Caused by: org.postgresql.util.PSQLException: FATAL: password authentication failed for user \"fixflow_user\"";

    public static final String STEP_JSON = "{\"order\":1,\"action\":\"Compare the configured password with the database role password.\","
            + "\"reason\":\"A mismatch causes this exact error.\",\"risk\":\"LOW\"}";

    private AiTestData() {
    }

    public static KnowledgeArticle pgAuthArticle() {
        return new KnowledgeArticle("pg-auth-failed", "PostgreSQL password authentication failed",
                ErrorCategory.DATABASE, "password authentication failed", "Explanation.",
                "Cause one\nCause two", "Check the config | Confirms what is sent | LOW",
                "Connect with psql", "Update the credentials.", "Built-in FixFlow guide");
    }

    public static String validAiJson() {
        return "{"
                + "\"summary\":\"The database rejected the login because the password did not match.\","
                + "\"category\":\"DATABASE\","
                + "\"severity\":\"HIGH\","
                + "\"confidenceLevel\":\"HIGH\","
                + "\"confidenceExplanation\":\"The log contains an explicit password authentication failure from PostgreSQL.\","
                + "\"probableCauses\":[{\"cause\":\"The configured database password does not match the role password.\","
                + "\"evidence\":[\"FATAL: password authentication failed for user fixflow_user\"],\"likelihood\":\"HIGH\"}],"
                + "\"recommendedSteps\":[" + STEP_JSON + "],"
                + "\"suggestedFix\":\"Update the application credentials to match the database role.\","
                + "\"verificationSteps\":[\"Restart the application and confirm the connection succeeds.\"],"
                + "\"knowledgeReferences\":[{\"articleId\":\"pg-auth-failed\",\"title\":\"ignored\"}],"
                + "\"limitations\":[\"The log does not show which configuration file supplies the password.\"],"
                + "\"needsMoreInformation\":false,"
                + "\"followUpQuestions\":[]"
                + "}";
    }

    public static String insufficientEvidenceJson(boolean withQuestion) {
        return "{"
                + "\"summary\":\"The text does not contain enough evidence for a diagnosis.\","
                + "\"category\":\"UNKNOWN\","
                + "\"severity\":\"UNKNOWN\","
                + "\"confidenceLevel\":\"LOW\","
                + "\"confidenceExplanation\":\"No exception or recognizable error pattern was found.\","
                + "\"probableCauses\":[],"
                + "\"recommendedSteps\":[],"
                + "\"suggestedFix\":\"\","
                + "\"verificationSteps\":[],"
                + "\"knowledgeReferences\":[],"
                + "\"limitations\":[\"Nothing could be concluded from the available text.\"],"
                + "\"needsMoreInformation\":true,"
                + "\"followUpQuestions\":[" + (withQuestion ? "\"Can you paste the full stack trace?\"" : "") + "]"
                + "}";
    }
}