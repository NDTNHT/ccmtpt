package com.taskmanagement.adapter.ai;

import com.taskmanagement.application.dto.ai.ProviderVerificationResultDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiProviderRegistryTest {

    private AiProviderRegistry registry;
    private AiConfig config;

    @BeforeEach
    void setUp() {
        config = AiConfig.defaults();
        registry = new AiProviderRegistry(config);
    }

    @Test
    @DisplayName("Registry should contain default real providers and NOT contain simulated provider")
    void testDefaultProviders() {
        List<CustomProviderDefinition> list = registry.listProviders();

        assertNotNull(list);
        assertEquals(5, list.size());
        assertTrue(list.stream().anyMatch(p -> "openai".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "anthropic".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "gemini".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "pollinations".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "ollama".equalsIgnoreCase(p.id())));
        assertFalse(list.stream().anyMatch(p -> "simulated".equalsIgnoreCase(p.id())));
    }

    @Test
    @DisplayName("Register custom Gemini provider and verify adapter creation")
    void testRegisterCustomGeminiProvider() {
        CustomProviderDefinition custom = CustomProviderDefinition.ofGemini(
            "my-gemini",
            "Google Gemini Flash",
            "https://generativelanguage.googleapis.com/v1beta",
            "AIzaSy-test-key",
            "gemini-2.5-flash"
        );

        registry.registerProvider(custom);
        registry.setActiveProviderId("my-gemini");

        CustomProviderDefinition active = registry.resolveActiveDefinition();
        assertEquals("my-gemini", active.id());
        assertEquals(AiProtocol.GEMINI, active.protocol());
        assertEquals("gemini-2.5-flash", active.defaultModel());

        var adapter = registry.createAdapterFor(active);
        assertInstanceOf(GeminiCompatibleAiAdapter.class, adapter);
    }

    @Test
    @DisplayName("Register custom OpenAI provider and activate it")
    void testRegisterCustomOpenAiProvider() {
        CustomProviderDefinition custom = CustomProviderDefinition.ofOpenAi(
            "custom-deepseek",
            "DeepSeek Chat",
            "https://api.deepseek.com/v1",
            "sk-deepseek-test-key",
            "deepseek-chat"
        );

        registry.registerProvider(custom);
        registry.setActiveProviderId("custom-deepseek");

        CustomProviderDefinition active = registry.resolveActiveDefinition();
        assertEquals("custom-deepseek", active.id());
        assertEquals(AiProtocol.OPENAI, active.protocol());
        assertEquals("deepseek-chat", active.defaultModel());

        var adapter = registry.createAdapterFor(active);
        assertInstanceOf(OpenAiCompatibleAiAdapter.class, adapter);
    }

    @Test
    @DisplayName("Register custom Anthropic provider and verify adapter creation")
    void testRegisterCustomAnthropicProvider() {
        CustomProviderDefinition custom = CustomProviderDefinition.ofAnthropic(
            "my-claude",
            "Company Claude",
            "https://api.anthropic.com/v1",
            "sk-ant-test-key",
            "claude-3-5-sonnet-20241022"
        );

        registry.registerProvider(custom);
        registry.setActiveProviderId("my-claude");

        CustomProviderDefinition active = registry.resolveActiveDefinition();
        assertEquals("my-claude", active.id());
        assertEquals(AiProtocol.ANTHROPIC, active.protocol());

        var adapter = registry.createAdapterFor(active);
        assertInstanceOf(AnthropicCompatibleAiAdapter.class, adapter);
    }

    @Test
    @DisplayName("Throw IllegalArgumentException when activating unknown provider")
    void testActivateUnknownProvider() {
        assertThrows(IllegalArgumentException.class, () -> registry.setActiveProviderId("non-existent-provider-id"));
    }

    @Test
    @DisplayName("Create adapter with empty key throws IllegalStateException")
    void testCreateAdapterThrowsWhenKeyEmpty() {
        CustomProviderDefinition emptyKeyDef = CustomProviderDefinition.ofAnthropic(
            "empty-claude", "No Key Claude", "https://api.anthropic.com/v1", "", "claude-3-5-sonnet-20241022"
        );

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.createAdapterFor(emptyKeyDef));
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Resolving active provider when no key is configured throws IllegalStateException")
    void testResolveActiveThrowsWhenNoKey() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> registry.resolveActiveDefinition());
        assertTrue(ex.getMessage().contains("No active AI provider connected"));
    }

    @Test
    @DisplayName("Verify provider with missing API key returns 400 error")
    void testVerifyProviderMissingKey() {
        ProviderVerificationResultDto result = registry.verifyAndDiscoverModels("https://api.openai.com/v1", "", AiProtocol.OPENAI, 5);
        assertFalse(result.valid());
        assertEquals(400, result.statusCode());
        assertTrue(result.message().contains("API key is required"));
    }

    @Test
    @DisplayName("Verify registered provider returns 404 when provider not found")
    void testVerifyRegisteredProviderNotFound() {
        ProviderVerificationResultDto result = registry.verifyRegisteredProvider("unknown-prov");
        assertFalse(result.valid());
        assertEquals(404, result.statusCode());
        assertTrue(result.message().contains("does not exist"));
    }

    @Test
    @DisplayName("Update provider default model successfully")
    void testUpdateProviderDefaultModel() {
        registry.updateProviderDefaultModel("pollinations", "openai/gpt-4o");
        CustomProviderDefinition updated = registry.getProvider("pollinations");
        assertNotNull(updated);
        assertEquals("openai/gpt-4o", updated.defaultModel());
    }
}
