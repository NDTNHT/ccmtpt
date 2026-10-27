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
    @DisplayName("Registry should contain default standard providers")
    void testDefaultProviders() {
        List<CustomProviderDefinition> list = registry.listProviders();

        assertNotNull(list);
        assertTrue(list.size() >= 5);
        assertTrue(list.stream().anyMatch(p -> "openai".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "anthropic".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "pollinations".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "ollama".equalsIgnoreCase(p.id())));
        assertTrue(list.stream().anyMatch(p -> "simulated".equalsIgnoreCase(p.id())));
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
    @DisplayName("Create adapter with empty key falls back to SimulatedAiAdapter")
    void testCreateAdapterFallbackWhenKeyEmpty() {
        CustomProviderDefinition emptyKeyDef = CustomProviderDefinition.ofAnthropic(
            "empty-claude", "No Key Claude", "https://api.anthropic.com/v1", "", "claude-3-5-sonnet-20241022"
        );

        var adapter = registry.createAdapterFor(emptyKeyDef);
        assertInstanceOf(SimulatedAiAdapter.class, adapter);
    }

    @Test
    @DisplayName("Verify simulated provider returns valid mock model list")
    void testVerifySimulatedProvider() {
        ProviderVerificationResultDto result = registry.verifyAndDiscoverModels("http://localhost", "", AiProtocol.SIMULATED, 5);
        assertTrue(result.valid());
        assertEquals(200, result.statusCode());
        assertEquals("SIMULATED", result.protocol());
        assertFalse(result.availableModels().isEmpty());
        assertTrue(result.availableModels().contains("heuristic-default"));
    }

    @Test
    @DisplayName("Verify registered provider returns 404 when provider not found")
    void testVerifyRegisteredProviderNotFound() {
        ProviderVerificationResultDto result = registry.verifyRegisteredProvider("unknown-prov");
        assertFalse(result.valid());
        assertEquals(404, result.statusCode());
        assertTrue(result.message().contains("không tồn tại") || result.message().contains("khong ton tai"));
    }

    @Test
    @DisplayName("Verify registered simulated provider updates available models")
    void testVerifyRegisteredSimulatedProviderUpdatesModels() {
        ProviderVerificationResultDto result = registry.verifyRegisteredProvider("simulated");
        assertTrue(result.valid());
        CustomProviderDefinition simulatedDef = registry.getProvider("simulated");
        assertNotNull(simulatedDef);
        assertFalse(simulatedDef.availableModels().isEmpty());
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
