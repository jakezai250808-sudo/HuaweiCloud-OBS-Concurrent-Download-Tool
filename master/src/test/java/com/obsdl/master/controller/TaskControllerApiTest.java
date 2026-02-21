package com.obsdl.master.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TaskControllerApiTest extends ApiIntegrationTestSupport {

    @Test
    void createTaskPersistsExpectedDatabaseFields() throws Exception {
        long accountId = insertObsAccount("acc-main", "ak-main", "sk-main", "obs-cn.example.com");

        long taskId = createTask(accountId, "  file-1.txt ", "file-1.txt", "file-2.txt");

        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT account_id, bucket, concurrency, total_objects, done_objects, status FROM download_task WHERE id = ?",
                taskId
        );
        assertEquals(accountId, ((Number) task.get("account_id")).longValue());
        assertEquals("bucket-main", task.get("bucket"));
        assertEquals(4, ((Number) task.get("concurrency")).intValue());
        assertEquals(2, ((Number) task.get("total_objects")).intValue());
        assertEquals(0, ((Number) task.get("done_objects")).intValue());
        assertEquals("CREATED", task.get("status"));

        Integer taskTimeNullCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM download_task WHERE id = ? AND (created_at IS NULL OR updated_at IS NULL)",
                Integer.class,
                taskId
        );
        assertEquals(0, taskTimeNullCount);

        Integer objectCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_object WHERE task_id = ?",
                Integer.class,
                taskId
        );
        assertEquals(2, objectCount);

        Integer pendingCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_object WHERE task_id = ? AND status = 'PENDING' AND size = 0",
                Integer.class,
                taskId
        );
        assertEquals(2, pendingCount);
    }

    @Test
    void leaseTaskUpdatesObjectLeaseFieldsAndReturnsCredentials() throws Exception {
        long accountId = insertObsAccount("acc-lease", "ak-lease", "sk-lease", "obs-hk.example.com");
        long taskId = createTask(accountId, "file-1.txt", "file-2.txt");

        postJson("/api/tasks/" + taskId + "/lease", Map.of("workerId", "worker-a", "count", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].objectKey").value("file-1.txt"))
                .andExpect(jsonPath("$.data[0].endpoint").value("obs-hk.example.com"))
                .andExpect(jsonPath("$.data[0].ak").value("ak-lease"))
                .andExpect(jsonPath("$.data[0].sk").value("sk-lease"))
                .andExpect(jsonPath("$.data[0].bucket").value("bucket-main"));

        Map<String, Object> object = jdbcTemplate.queryForMap(
                "SELECT status, leased_by FROM task_object WHERE task_id = ? AND object_key = 'file-1.txt'",
                taskId
        );
        assertEquals("LEASED", object.get("status"));
        assertEquals("worker-a", object.get("leased_by"));
    }

    @Test
    void reportDoneUpdatesObjectAndTaskCompletionFields() throws Exception {
        long accountId = insertObsAccount("acc-done", "ak-done", "sk-done", "obs-sz.example.com");
        long taskId = createTask(accountId, "done-file.txt");

        postJson("/api/tasks/" + taskId + "/lease", Map.of("workerId", "worker-done", "count", 1))
                .andExpect(status().isOk());

        postJson("/api/tasks/" + taskId + "/report", Map.of(
                "workerId", "worker-done",
                "objectKey", "done-file.txt",
                "status", "done",
                "errorMessage", ""
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> object = jdbcTemplate.queryForMap(
                "SELECT status, leased_by FROM task_object WHERE task_id = ? AND object_key = 'done-file.txt'",
                taskId
        );
        assertEquals("DONE", object.get("status"));
        assertEquals("worker-done", object.get("leased_by"));

        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT done_objects, total_objects, status FROM download_task WHERE id = ?",
                taskId
        );
        assertEquals(1, ((Number) task.get("done_objects")).intValue());
        assertEquals(1, ((Number) task.get("total_objects")).intValue());
        assertEquals("DONE", task.get("status"));
    }

    @Test
    void reportFailedMarksObjectFailedWithoutIncreasingDoneCount() throws Exception {
        long accountId = insertObsAccount("acc-failed", "ak-failed", "sk-failed", "obs-gz.example.com");
        long taskId = createTask(accountId, "failed-file.txt");

        postJson("/api/tasks/" + taskId + "/lease", Map.of("workerId", "worker-fail", "count", 1))
                .andExpect(status().isOk());

        postJson("/api/tasks/" + taskId + "/report", Map.of(
                "workerId", "worker-fail",
                "objectKey", "failed-file.txt",
                "status", "failed",
                "errorMessage", "network timeout"
        )).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Map<String, Object> object = jdbcTemplate.queryForMap(
                "SELECT status, leased_by FROM task_object WHERE task_id = ? AND object_key = 'failed-file.txt'",
                taskId
        );
        assertEquals("FAILED", object.get("status"));
        assertEquals("worker-fail", object.get("leased_by"));

        Map<String, Object> task = jdbcTemplate.queryForMap(
                "SELECT done_objects, total_objects, status FROM download_task WHERE id = ?",
                taskId
        );
        assertEquals(0, ((Number) task.get("done_objects")).intValue());
        assertEquals(1, ((Number) task.get("total_objects")).intValue());
        assertEquals("CREATED", task.get("status"));
    }

    @Test
    void getTaskAndListObjectsReturnsExpectedPayload() throws Exception {
        long accountId = insertObsAccount("acc-query", "ak-query", "sk-query", "obs-query.example.com");
        long taskId = createTask(accountId, "obj-a.txt", "obj-b.txt");

        getJson("/api/tasks/" + taskId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(taskId))
                .andExpect(jsonPath("$.data.accountId").value(accountId))
                .andExpect(jsonPath("$.data.bucket").value("bucket-main"))
                .andExpect(jsonPath("$.data.status").value("CREATED"))
                .andExpect(jsonPath("$.data.objects.length()").value(2));

        getJson("/api/tasks/" + taskId + "/objects")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"));
    }

    @Test
    void createTaskReturnsNotFoundWhenAccountDoesNotExist() throws Exception {
        Map<String, Object> request = Map.of(
                "accountId", 999999L,
                "bucket", "bucket-main",
                "selection", Map.of("objects", List.of("obj-a.txt"))
        );

        postJson("/api/tasks", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(40403))
                .andExpect(jsonPath("$.message").value("账号不存在"));
    }

    private long createTask(long accountId, String... objects) throws Exception {
        List<String> objectList = Arrays.asList(objects);
        Map<String, Object> request = Map.of(
                "accountId", accountId,
                "bucket", "bucket-main",
                "selection", Map.of("objects", objectList)
        );

        MvcResult result = postJson("/api/tasks", request)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.path("data").path("taskId").asLong();
    }
}
