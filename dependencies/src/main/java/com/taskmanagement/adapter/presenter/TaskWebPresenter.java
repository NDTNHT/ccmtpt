package com.taskmanagement.adapter.presenter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.application.boundary.TaskShowing;
import com.taskmanagement.application.dto.TaskOutputDTO;

import java.io.PrintStream;

/**
 * Serializes task output to JSON using Jackson.
 */
public class TaskWebPresenter implements TaskShowing {
    private final ObjectMapper objectMapper;
    private final PrintStream out;
    private String jsonOutput;

    public TaskWebPresenter() {
        this(System.out);
    }

    public TaskWebPresenter(PrintStream out) {
        this.out = out != null ? out : System.out;
        this.objectMapper = new ObjectMapper().findAndRegisterModules();
    }

    @Override
    public void showResult(TaskOutputDTO outputData) {
        try {
            this.jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(outputData);
            out.println("[WEB JSON RESPONSE SENT TO BROWSER]:\n" + this.jsonOutput);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize task output to JSON.", exception);
        }
    }

    public String getJsonOutput() {
        return jsonOutput;
    }
}
