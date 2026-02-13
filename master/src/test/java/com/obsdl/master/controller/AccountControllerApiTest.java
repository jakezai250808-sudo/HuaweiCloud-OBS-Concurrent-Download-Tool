package com.obsdl.master.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AccountControllerApiTest extends ApiIntegrationTestSupport {

    @Test
    void accountLifecycleViaApi() throws Exception {
        Map<String, Object> create = Map.of(
                "name", "acc-1",
                "accessKey", "ak-1",
                "secretKey", "sk-1",
                "endpoint", "obs-cn.example.com",
                "bucket", "bucket-a"
        );

        postJson("/api/accounts", create)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("acc-1"))
                .andExpect(jsonPath("$.data.accessKey").value("ak-1"))
                .andExpect(jsonPath("$.data.endpoint").value("obs-cn.example.com"))
                .andExpect(jsonPath("$.data.bucket").value("bucket-a"));

        getJson("/api/accounts")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("acc-1"));

        Map<String, Object> update = Map.of(
                "id", 1,
                "name", "acc-1-updated",
                "accessKey", "ak-2",
                "secretKey", "sk-2",
                "endpoint", "obs-hk.example.com",
                "bucket", "bucket-b"
        );

        putJson("/api/accounts", update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("acc-1-updated"))
                .andExpect(jsonPath("$.data.bucket").value("bucket-b"));

        deleteJson("/api/accounts?id=1")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        getJson("/api/accounts")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void updateMissingAccountReturnsBizError() throws Exception {
        Map<String, Object> update = Map.of(
                "id", 404,
                "name", "missing",
                "accessKey", "ak",
                "secretKey", "sk",
                "endpoint", "obs.example.com",
                "bucket", "bucket"
        );

        putJson("/api/accounts", update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40401))
                .andExpect(jsonPath("$.message").value("账户不存在"));
    }
}
