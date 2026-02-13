package com.obsdl.master.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObsMockControllerApiTest extends ApiIntegrationTestSupport {

    @Test
    void listBucketsViaApi() throws Exception {
        getJson("/api/obs/buckets")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.buckets.length()").value(3))
                .andExpect(jsonPath("$.data.buckets[0]").value("demo-bucket-a"));
    }

    @Test
    void listObjectsViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "bucket-x"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bucket").value("bucket-x"))
                .andExpect(jsonPath("$.data.objectKeys.length()").value(3));
    }

    @Test
    void listObjectsWithoutBucketReturnsValidationError() throws Exception {
        getJson("/api/obs/objects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));
    }
}
