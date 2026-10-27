package com.taskmanagement.adapter.ai;

/**
 * Standard AI communication protocols supported by the system.
 */
public enum AiProtocol {
    /**
     * Standard OpenAI Chat Completions API (/chat/completions) with Bearer token authentication.
     * Compatible with OpenAI, DeepSeek, Ollama, Groq, OpenRouter, Pollinations, vLLM, etc.
     */
    OPENAI,

    /**
     * Standard Anthropic Messages API (/messages) with x-api-key and anthropic-version headers.
     * Compatible with Claude 3.5 Sonnet, Claude 3 Opus/Haiku, and Anthropic gateways.
     */
    ANTHROPIC,

    /**
     * Deterministic, offline heuristic engine without external HTTP requests.
     */
    SIMULATED
}
