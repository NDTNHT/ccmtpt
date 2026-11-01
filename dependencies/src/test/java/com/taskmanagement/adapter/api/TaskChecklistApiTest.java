package com.taskmanagement.adapter.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanagement.adapter.persistence.IOSaveToSQLite;
import com.taskmanagement.adapter.persistence.IOSaveToSQLiteProject;
import com.taskmanagement.application.control.TaskControl;
import com.taskmanagement.infrastructure.event.InMemoryDomainEventDispatcher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.nio.file.Path;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TaskChecklistApiTest {
    @TempDir Path directory;

    @Test
    void checklistSurvivesApiWritesAndDatabaseReopen() throws Exception {
        Path db = directory.resolve("tasks.db");
        TaskControl control = new TaskControl(new IOSaveToSQLite(db), new IOSaveToSQLiteProject(db), new InMemoryDomainEventDispatcher());
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new TaskApiController(control))
            .setControllerAdvice(new GlobalApiExceptionHandler()).build();
        String response = mvc.perform(post("/api/tasks").contentType("application/json")
            .content("{\"title\":\"Prepare report\",\"subTasks\":[\"Draft\",\"Review\"]}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.data.subTasks.length()").value(2))
            .andReturn().getResponse().getContentAsString();
        JsonNode task = new ObjectMapper().readTree(response).get("data");
        String id = task.get("taskId").asText();
        String subId = task.get("subTasks").get(0).get("id").asText();
        mvc.perform(patch("/api/tasks/" + id + "/subtasks/" + subId).contentType("application/json")
            .content("{\"completed\":true}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.subTasks[0].completed").value(true));
        TaskControl reopened = new TaskControl(new IOSaveToSQLite(db), new IOSaveToSQLiteProject(db), new InMemoryDomainEventDispatcher());
        MockMvc reopenedMvc = MockMvcBuilders.standaloneSetup(new TaskApiController(reopened)).build();
        reopenedMvc.perform(get("/api/tasks/" + id)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.subTasks[0].completed").value(true));
        mvc.perform(patch("/api/tasks/" + id + "/subtasks/" + subId).contentType("application/json").content("{}"))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/tasks").contentType("application/json")
            .content("{\"title\":\"Invalid report\",\"subTasks\":[\"Draft\",\"draft\"]}"))
            .andExpect(status().isUnprocessableEntity());
    }
}
