package com.taskmanagement.adapter.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.adapter.ai.dto.AiPromptOverrideRequest;
import com.taskmanagement.adapter.api.GlobalApiExceptionHandler;
import com.taskmanagement.application.ai.AiTaskAssistantInputBoundary;
import com.taskmanagement.application.ai.dto.AiPriorityProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiSubTaskProposalOutputDTO;
import com.taskmanagement.application.ai.dto.AiSummaryOutputDTO;
import com.taskmanagement.application.ai.dto.AiTaskBreakdownOutputDTO;
import com.taskmanagement.application.exception.TaskNotFoundException;
import com.taskmanagement.domain.task.enums.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("AiTaskApiController Integration Tests")
class AiTaskApiControllerTest {

    private MockMvc mockMvc;
    private AiTaskAssistantInputBoundary aiAssistant;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        aiAssistant = mock(AiTaskAssistantInputBoundary.class);
        objectMapper = new ObjectMapper();
        AiTaskApiController controller = new AiTaskApiController(aiAssistant);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalApiExceptionHandler())
            .build();
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/ai-breakdown returns breakdown preview when apply=false")
    void shouldReturnBreakdownPreview() throws Exception {
        UUID taskId = UUID.randomUUID();
        Instant deadline = Instant.parse("2026-11-05T12:00:00Z");
        AiTaskBreakdownOutputDTO dto = new AiTaskBreakdownOutputDTO(
            taskId,
            List.of(new AiSubTaskProposalOutputDTO("Analyze schema", "Database review")),
            deadline,
            "Decomposed into 1 item",
            false
        );

        when(aiAssistant.breakdownTask(eq(taskId), eq(false), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-breakdown", taskId)
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Task breakdown preview generated successfully"))
            .andExpect(jsonPath("$.data.taskId").value(taskId.toString()))
            .andExpect(jsonPath("$.data.applied").value(false))
            .andExpect(jsonPath("$.data.subtasks[0].title").value("Analyze schema"))
            .andExpect(jsonPath("$.data.reasoning").value("Decomposed into 1 item"));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/ai-breakdown?apply=true returns applied status")
    void shouldReturnAppliedBreakdown() throws Exception {
        UUID taskId = UUID.randomUUID();
        AiTaskBreakdownOutputDTO dto = new AiTaskBreakdownOutputDTO(
            taskId,
            List.of(new AiSubTaskProposalOutputDTO("Write tests", "Test suite")),
            Instant.now(),
            "Applied breakdown",
            true
        );

        when(aiAssistant.breakdownTask(eq(taskId), eq(true), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-breakdown", taskId)
                .param("apply", "true")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.message").value("Task breakdown generated and applied successfully"))
            .andExpect(jsonPath("$.data.applied").value(true));
    }

    @Test
    @DisplayName("POST /api/tasks/{id}/ai-breakdown (without v1) works as alias")
    void shouldSupportPathWithoutV1() throws Exception {
        UUID taskId = UUID.randomUUID();
        AiTaskBreakdownOutputDTO dto = new AiTaskBreakdownOutputDTO(
            taskId,
            List.of(),
            null,
            "Preview",
            false
        );

        when(aiAssistant.breakdownTask(eq(taskId), eq(false), any())).thenReturn(dto);

        mockMvc.perform(post("/api/tasks/{taskId}/ai-breakdown", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/ai-prioritize evaluates and returns priority proposal")
    void shouldReturnPriorityProposal() throws Exception {
        UUID taskId = UUID.randomUUID();
        AiPriorityProposalOutputDTO dto = new AiPriorityProposalOutputDTO(
            taskId,
            Priority.HIGH,
            "Urgent production bug",
            0.95,
            false
        );

        when(aiAssistant.prioritizeTask(eq(taskId), eq(false), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-prioritize", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.suggestedPriority").value("HIGH"))
            .andExpect(jsonPath("$.data.confidence").value(0.95))
            .andExpect(jsonPath("$.data.applied").value(false));
    }

    @Test
    @DisplayName("POST /api/v1/tasks/{id}/ai-summary returns task summary")
    void shouldReturnTaskSummary() throws Exception {
        UUID taskId = UUID.randomUUID();
        AiSummaryOutputDTO dto = new AiSummaryOutputDTO(
            taskId,
            "Task Title",
            "This is a task summary",
            List.of("Key point 1", "Key point 2"),
            "Start execution immediately"
        );

        when(aiAssistant.summarizeTask(eq(taskId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-summary", taskId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.summary").value("This is a task summary"))
            .andExpect(jsonPath("$.data.suggestedNextStep").value("Start execution immediately"))
            .andExpect(jsonPath("$.data.keyHighlights[0]").value("Key point 1"));
    }

    @Test
    @DisplayName("Accepts custom prompt override request in body")
    void shouldAcceptPromptOverrideRequestBody() throws Exception {
        UUID taskId = UUID.randomUUID();
        AiPromptOverrideRequest request = new AiPromptOverrideRequest(
            "Custom system prompt",
            "Focus only on performance tests"
        );
        AiSummaryOutputDTO dto = new AiSummaryOutputDTO(
            taskId,
            "Task Title",
            "Performance summary",
            List.of(),
            "Run JMH benchmark"
        );

        when(aiAssistant.summarizeTask(eq(taskId), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-summary", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Accepts custom provider configuration in request body")
    void shouldAcceptCustomProviderInRequestBody() throws Exception {
        UUID taskId = UUID.randomUUID();
        String jsonPayload = """
            {
              "systemPrompt": "Expert assistant",
              "provider": "custom",
              "customFormat": "anthropic",
              "customBaseUrl": "https://api.anthropic.com/v1",
              "customApiKey": "sk-ant-test",
              "customModel": "claude-3-5-sonnet-20241022",
              "customHeaders": {
                "anthropic-version": "2023-06-01"
              }
            }
            """;

        AiTaskBreakdownOutputDTO dto = new AiTaskBreakdownOutputDTO(
            taskId,
            List.of(),
            null,
            "Custom provider breakdown",
            false
        );

        when(aiAssistant.breakdownTask(eq(taskId), eq(false), any())).thenReturn(dto);

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-breakdown", taskId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonPayload))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.reasoning").value("Custom provider breakdown"));
    }

    @Test
    @DisplayName("Returns 404 NOT_FOUND when task does not exist")
    void shouldReturn404WhenTaskNotFound() throws Exception {
        UUID nonExistentId = UUID.randomUUID();
        when(aiAssistant.breakdownTask(eq(nonExistentId), eq(false), any()))
            .thenThrow(new TaskNotFoundException("Task not found with ID: " + nonExistentId));

        mockMvc.perform(post("/api/v1/tasks/{taskId}/ai-breakdown", nonExistentId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"))
            .andExpect(jsonPath("$.error.details").value("Task not found with ID: " + nonExistentId));
    }
}
