package com.taskmanagement.adapter.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiConfigTest {

    @Test
    @DisplayName("Default configuration should have standard defaults and no valid API key")
    void testDefaults() {
        AiConfig config = AiConfig.defaults();

        assertTrue(config.isEnabled());
        assertEquals("auto", config.getProvider());
        assertEquals("https://api.openai.com/v1", config.getBaseUrl());
        assertEquals("", config.getApiKey());
        assertEquals("gpt-4o-mini", config.getModel());
        assertEquals(0.3, config.getTemperature());
        assertEquals(30, config.getTimeoutSeconds());
        assertFalse(config.hasValidApiKey());
        assertEquals(AiConfig.DEFAULT_SYSTEM_PROMPT, config.getSystemPrompt());
    }

    @Test
    @DisplayName("Simulation configuration should have simulated provider")
    void testForSimulation() {
        AiConfig config = AiConfig.forSimulation();

        assertEquals("simulated", config.getProvider());
    }

    @Test
    @DisplayName("Resolve effective prompt should favor custom write-in prompt over configured default")
    void testResolveEffectiveSystemPrompt() {
        AiConfig config = AiConfig.defaults();
        String defaultPrompt = config.getSystemPrompt();

        assertEquals(defaultPrompt, config.resolveEffectiveSystemPrompt(null));
        assertEquals(defaultPrompt, config.resolveEffectiveSystemPrompt("   "));

        String customWriteIn = "You are a specialized financial software planner.";
        assertEquals(customWriteIn, config.resolveEffectiveSystemPrompt(customWriteIn));
    }

    @Test
    @DisplayName("BaseUrl should strip trailing slashes")
    void testBaseUrlTrailingSlash() {
        AiConfig config = new AiConfig();
        config.setBaseUrl("http://localhost:11434/v1///");

        assertEquals("http://localhost:11434/v1", config.getBaseUrl());
    }

    @Test
    @DisplayName("hasValidApiKey returns true when apiKey is present")
    void testHasValidApiKey() {
        AiConfig config = AiConfig.of("sk-test-key-123", "gpt-4o");

        assertTrue(config.hasValidApiKey());
        assertEquals("sk-test-key-123", config.getApiKey());
        assertEquals("gpt-4o", config.getModel());
    }
}
