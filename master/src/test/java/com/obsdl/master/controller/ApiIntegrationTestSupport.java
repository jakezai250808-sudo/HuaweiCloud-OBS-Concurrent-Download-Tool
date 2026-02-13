package com.obsdl.master.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
abstract class ApiIntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("DELETE FROM task_object");
        jdbcTemplate.execute("DELETE FROM download_task");
        jdbcTemplate.execute("DELETE FROM worker_node");
        jdbcTemplate.execute("DELETE FROM obs_account");
    }

    protected ResultActions postJson(String url, Object body) throws Exception {
        return mockMvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions putJson(String url, Object body) throws Exception {
        return mockMvc.perform(put(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected ResultActions getJson(String url) throws Exception {
        return mockMvc.perform(get(url).accept(MediaType.APPLICATION_JSON));
    }

    protected ResultActions deleteJson(String url) throws Exception {
        return mockMvc.perform(delete(url).accept(MediaType.APPLICATION_JSON));
    }

    protected long insertObsAccount(String accountName, String accessKey, String secretKey, String endpoint) {
        jdbcTemplate.update("""
                INSERT INTO obs_account (account_name, access_key, secret_key, endpoint, created_at, updated_at)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, accountName, accessKey, secretKey, endpoint);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM obs_account WHERE account_name = ?",
                Long.class,
                accountName
        );
        if (id == null) {
            throw new IllegalStateException("failed to insert obs_account");
        }
        return id;
    }
}
