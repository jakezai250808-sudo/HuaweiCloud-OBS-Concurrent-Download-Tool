package com.obsdl.master.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObsMockControllerApiTest extends ApiIntegrationTestSupport {

    @Test
    void listBucketsViaApi() throws Exception {
        long accountId = insertObsAccount("acc-bucket-query", "ak-bucket-query", "sk-bucket-query", "obs-cn.example.com", "demo-bucket");
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/buckets")
                        .param("accountId", String.valueOf(accountId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.buckets").isArray())
                .andExpect(jsonPath("$.data.buckets").value(org.hamcrest.Matchers.hasItem("demo-bucket")));
    }

    @Test
    void rootListingViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "demo-bucket")
                        .param("prefix", ""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.bucket").value("demo-bucket"))
                .andExpect(jsonPath("$.data.prefix").value(""))
                .andExpect(jsonPath("$.data.delimiter").value("/"))
                .andExpect(jsonPath("$.data.directories").isArray())
                .andExpect(jsonPath("$.data.directories[*].prefix").value(org.hamcrest.Matchers.hasItems("docs/", "photos/")))
                .andExpect(jsonPath("$.data.objects[*].key").value(org.hamcrest.Matchers.hasItem("root.txt")))
                .andExpect(jsonPath("$.data.objects[0].lastModified").value(org.hamcrest.Matchers.matchesPattern("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(?:Z|[+-]\\d{2}:\\d{2})$")));
    }

    @Test
    void level1ListingViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "demo-bucket")
                        .param("prefix", "docs/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prefix").value("docs/"))
                .andExpect(jsonPath("$.data.directories[*].prefix").value(org.hamcrest.Matchers.hasItem("docs/specs/")))
                .andExpect(jsonPath("$.data.objects[*].key").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("docs/specs/v1/api.yaml"))));
    }

    @Test
    void level2ListingViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "demo-bucket")
                        .param("prefix", "docs/specs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prefix").value("docs/specs/"))
                .andExpect(jsonPath("$.data.directories[*].prefix").value(org.hamcrest.Matchers.hasItems("docs/specs/v1/", "docs/specs/v2/")));
    }

    @Test
    void leafListingViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "demo-bucket")
                        .param("prefix", "docs/specs/v1/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prefix").value("docs/specs/v1/"))
                .andExpect(jsonPath("$.data.directories.length()").value(0))
                .andExpect(jsonPath("$.data.objects[*].key").value(org.hamcrest.Matchers.hasItems(
                        "docs/specs/v1/api.yaml",
                        "docs/specs/v1/design.pdf"
                )));
    }

    @Test
    void emptyListingViaApi() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/api/obs/objects")
                        .param("bucket", "demo-bucket")
                        .param("prefix", "no_such_dir/"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.prefix").value("no_such_dir/"))
                .andExpect(jsonPath("$.data.directories.length()").value(0))
                .andExpect(jsonPath("$.data.objects.length()").value(0));
    }
}
