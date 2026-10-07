package com.careersphere.ai;

public interface AiProvider {
    boolean isConfigured();
    String generateJson(String prompt);
}
