package com.taskmanagement.adapter.ai;

/**
 * Configuration settings for AI Assistant integration, loaded from properties or environment.
 */
public class AiConfig {
    public static final String DEFAULT_SYSTEM_PROMPT =
        "You are an expert AI Task & Project Management Assistant. " +
        "Analyze software tasks, decompose them into actionable, concise checklist subtasks, " +
        "estimate realistic deadlines based on scope, evaluate task priority combining urgency and complexity, " +
        "and provide clear executive summaries.";

    private boolean enabled = true;
    private String provider = "auto";
    private String baseUrl = "https://api.openai.com/v1";
    private String apiKey = "";
    private String model = "gpt-4o-mini";
    private double temperature = 0.3;
    private int timeoutSeconds = 30;
    private String systemPrompt = DEFAULT_SYSTEM_PROMPT;

    public AiConfig() {
    }

    public static AiConfig defaults() {
        return new AiConfig();
    }

    public static AiConfig forSimulation() {
        AiConfig config = new AiConfig();
        config.setProvider("simulated");
        return config;
    }

    public static AiConfig of(String apiKey, String model) {
        AiConfig config = new AiConfig();
        config.setApiKey(apiKey);
        if (model != null && !model.isBlank()) {
            config.setModel(model);
        }
        return config;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl != null ? baseUrl.replaceAll("/+$", "") : "https://api.openai.com/v1";
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model != null && !model.isBlank() ? model : "gpt-4o-mini";
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public void setTimeoutSeconds(int timeoutSeconds) {
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 30;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = (systemPrompt != null && !systemPrompt.isBlank())
            ? systemPrompt
            : DEFAULT_SYSTEM_PROMPT;
    }

    public String resolveEffectiveSystemPrompt(String customWriteInPrompt) {
        if (customWriteInPrompt != null && !customWriteInPrompt.isBlank()) {
            return customWriteInPrompt.trim();
        }
        return systemPrompt;
    }

    public boolean hasValidApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
