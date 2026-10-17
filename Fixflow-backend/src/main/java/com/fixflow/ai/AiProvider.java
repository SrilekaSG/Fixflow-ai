package com.fixflow.ai;

public interface AiProvider {

    /** True only when the provider is fully configured and can be called. */
    boolean isAvailable();

    String providerName();

    String modelName();

    /** Returns the raw text reply. Throws AiProviderException with a user-safe message on any failure. */
    String generate(String systemPrompt, String userPrompt);
}