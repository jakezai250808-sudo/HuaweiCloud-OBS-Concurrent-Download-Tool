package com.obsdl.master.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenApiDocExportTest extends ApiIntegrationTestSupport {

    @Test
    void exportOpenApiDocToRepository() throws Exception {
        MvcResult result = getJson("/v3/api-docs")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);

        Path output = Path.of("..", "docs", "api", "openapi.json").toAbsolutePath().normalize();
        Files.createDirectories(output.getParent());
        Files.writeString(
                output,
                prettyJson + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
    }
}
