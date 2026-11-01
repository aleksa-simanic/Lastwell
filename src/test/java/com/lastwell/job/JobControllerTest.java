package com.lastwell.job;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthReturnsOk() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ok"))
                .andExpect(jsonPath("$.service").value("Lastwell"));
    }

    @Test
    void createWorkflowAndDeleteJob() throws Exception {
        String payload = """
                {
                  "code": "lw-300",
                  "customer": "Jordan Lee",
                  "footwear": "oxford",
                  "leather": "calf",
                  "issue": "Worn heel and loose welt",
                  "cobbler": "Amina Cole",
                  "dueOn": "2026-09-05",
                  "status": "INTAKE",
                  "notes": "Match existing lift.",
                  "tags": ["dress", "rush"]
                }
                """;

        MvcResult created = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("LW-300"))
                .andExpect(jsonPath("$.status").value("INTAKE"))
                .andExpect(jsonPath("$.footwear").value("oxford"))
                .andReturn();

        JsonNode body = objectMapper.readTree(created.getResponse().getContentAsString());
        String id = body.get("id").asText();
        assertTrue(id.length() > 10);

        mockMvc.perform(post("/api/v1/jobs/" + id + "/last"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LASTING"));

        mockMvc.perform(post("/api/v1/jobs/" + id + "/stitch"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("STITCHING"));

        mockMvc.perform(post("/api/v1/jobs/" + id + "/finish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINISHING"));

        mockMvc.perform(post("/api/v1/jobs/" + id + "/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(post("/api/v1/jobs/" + id + "/pickup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PICKED_UP"));

        mockMvc.perform(put("/api/v1/jobs/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "notes": "Picked up with receipt."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes").value("Picked up with receipt."));

        mockMvc.perform(delete("/api/v1/jobs/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/jobs/" + id))
                .andExpect(status().isNotFound());
    }

    @Test
    void listSupportsFilters() throws Exception {
        mockMvc.perform(get("/api/v1/jobs")
                        .param("status", "READY")
                        .param("footwear", "loafer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void validationFailureReturnsBadRequest() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/jobs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "",
                                  "customer": "Sam Ortega",
                                  "footwear": "oxford",
                                  "leather": "calf",
                                  "issue": "Loose welt",
                                  "cobbler": "Amina Cole",
                                  "dueOn": "2026-09-01",
                                  "status": "INTAKE"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andReturn();

        String detail = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("detail")
                .asText()
                .toLowerCase();
        assertTrue(detail.contains("code"));
    }
}
