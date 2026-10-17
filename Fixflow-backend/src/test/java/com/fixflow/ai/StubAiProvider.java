package com.fixflow.ai;

import com.fixflow.exception.AiProviderException;
import com.fixflow.exception.AiProviderException.Reason;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A fake AI provider for tests: replies come from a queue and every prompt is recorded. */
public class StubAiProvider implements AiProvider {

    private final Deque<Object> queue = new ArrayDeque<>();
    private final List<String> systemPrompts = new ArrayList<>();
    private final List<String> userPrompts = new ArrayList<>();
    private boolean available = true;

    public StubAiProvider enqueue(String response) {
        queue.add(response);
        return this;
    }

    public StubAiProvider enqueueFailure(AiProviderException failure) {
        queue.add(failure);
        return this;
    }

    public StubAiProvider setAvailable(boolean available) {
        this.available = available;
        return this;
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    @Override
    public String providerName() {
        return "stub";
    }

    @Override
    public String modelName() {
        return "stub-model";
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        systemPrompts.add(systemPrompt);
        userPrompts.add(userPrompt);
        Object next = queue.poll();
        if (next == null) {
            throw new AiProviderException(Reason.UNAVAILABLE, "No stub response was queued.");
        }
        if (next instanceof AiProviderException failure) {
            throw failure;
        }
        return (String) next;
    }

    public int callCount() {
        return userPrompts.size();
    }

    public List<String> systemPrompts() {
        return systemPrompts;
    }

    public List<String> userPrompts() {
        return userPrompts;
    }
    public StubAiProvider reset() {
        queue.clear();
        systemPrompts.clear();
        userPrompts.clear();
        available = true;
        return this;
    }
}