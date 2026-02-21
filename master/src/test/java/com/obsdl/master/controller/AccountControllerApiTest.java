package com.obsdl.master.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
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

        MvcResult createResult = postJson("/api/accounts", create)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").isNumber())
                .andExpect(jsonPath("$.data.name").value("acc-1"))
                .andExpect(jsonPath("$.data.accessKey").value("ak-1"))
                .andExpect(jsonPath("$.data.endpoint").value("obs-cn.example.com"))
                .andExpect(jsonPath("$.data.bucket").value("bucket-a"))
                .andReturn();
        JsonNode createRoot = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long accountId = createRoot.path("data").path("id").asLong();

        getJson("/api/accounts")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value("acc-1"));

        Map<String, Object> update = Map.of(
                "id", accountId,
                "name", "acc-1-updated",
                "accessKey", "ak-2",
                "secretKey", "sk-2",
                "endpoint", "obs-hk.example.com",
                "bucket", "bucket-b"
        );

        putJson("/api/accounts", update)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(accountId))
                .andExpect(jsonPath("$.data.name").value("acc-1-updated"))
                .andExpect(jsonPath("$.data.bucket").value("bucket-b"));

        deleteJson("/api/accounts?id=" + accountId)
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

    @Test
    void createWithBlankFieldsReturnsReadableValidationMessages() throws Exception {
        Map<String, Object> create = Map.of(
                "name", "",
                "accessKey", "",
                "secretKey", "",
                "endpoint", "",
                "bucket", ""
        );

        postJson("/api/accounts", create)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message", containsString("name 不能为空")))
                .andExpect(jsonPath("$.message", containsString("accessKey 不能为空")))
                .andExpect(jsonPath("$.message", containsString("secretKey 不能为空")))
                .andExpect(jsonPath("$.message", containsString("endpoint 不能为空")))
                .andExpect(jsonPath("$.message", containsString("bucket 不能为空")))
                .andExpect(jsonPath("$.message", not(containsString("不能为空; 不能为空"))));
    }
}
