package com.obsdl.master.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WorkerControllerApiTest extends ApiIntegrationTestSupport {

    @Test
    void registerAndHeartbeatViaApi() throws Exception {
        Map<String, Object> register = Map.of(
                "workerId", "worker-1",
                "host", "127.0.0.1",
                "port", 19090,
                "maxConcurrency", 8
        );

        postJson("/api/workers/register", register)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.workerId").value("worker-1"))
                .andExpect(jsonPath("$.data.activeTasks").value(0))
                .andExpect(jsonPath("$.data.queuedTasks").value(0));

        Map<String, Object> heartbeat = Map.of(
                "workerId", "worker-1",
                "activeTasks", 2,
                "queuedTasks", 5
        );

        postJson("/api/workers/heartbeat", heartbeat)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.workerId").value("worker-1"))
                .andExpect(jsonPath("$.data.activeTasks").value(2))
                .andExpect(jsonPath("$.data.queuedTasks").value(5));
    }

    @Test
    void heartbeatWithoutRegisterReturnsBizError() throws Exception {
        Map<String, Object> heartbeat = Map.of(
                "workerId", "missing-worker",
                "activeTasks", 1,
                "queuedTasks", 1
        );

        postJson("/api/workers/heartbeat", heartbeat)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40402))
                .andExpect(jsonPath("$.message").value("Worker 未注册"));
    }

    @Test
    void registerWithBlankWorkerIdReturnsValidationError() throws Exception {
        Map<String, Object> register = Map.of(
                "workerId", "   ",
                "host", "127.0.0.1",
                "port", 19090,
                "maxConcurrency", 8
        );

        postJson("/api/workers/register", register)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));
    }

    @Test
    void heartbeatMissingQueuedTasksReturnsValidationError() throws Exception {
        postJson("/api/workers/register", Map.of(
                "workerId", "worker-2",
                "host", "127.0.0.1",
                "port", 19091,
                "maxConcurrency", 4
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        postJson("/api/workers/heartbeat", Map.of(
                "workerId", "worker-2",
                "activeTasks", 1
        ))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40001));
    }

}
