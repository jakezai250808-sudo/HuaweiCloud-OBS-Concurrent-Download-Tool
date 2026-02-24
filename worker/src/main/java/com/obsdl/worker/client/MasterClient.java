package com.obsdl.worker.client;

import com.obsdl.worker.config.WorkerRuntimeProperties;
import com.obsdl.worker.dto.*;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Component
public class MasterClient {

    private final RestTemplate restTemplate;
    private final WorkerRuntimeProperties props;

    public MasterClient(RestTemplateBuilder builder, WorkerRuntimeProperties props) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
        this.props = props;
    }

    public void register(String host, int port) {
        post("/api/workers/register", new WorkerRegisterRequest(
                props.workerId(),
                host,
                port,
                props.maxDlConcurrency()
        ), new ParameterizedTypeReference<ApiResponse<Object>>() {});
    }

    public void heartbeat(int activeTasks, int queuedTasks) {
        post("/api/workers/heartbeat", new WorkerHeartbeatRequest(
                props.workerId(),
                activeTasks,
                queuedTasks
        ), new ParameterizedTypeReference<ApiResponse<Object>>() {});
    }

    public List<MasterTaskResponse> listTasks() {
        ApiResponse<List<MasterTaskResponse>> response = get(
                "/api/tasks",
                new ParameterizedTypeReference<ApiResponse<List<MasterTaskResponse>>>() {}
        );
        if (response == null || response.code() == null || response.code() != 0) {
            return List.of();
        }
        return Objects.requireNonNullElse(response.data(), List.of());
    }

    public List<TaskLeaseObjectResponse> lease(Long taskId) {
        String path = "/api/tasks/%d/lease".formatted(taskId);
        ApiResponse<List<TaskLeaseObjectResponse>> response = post(path,
                new TaskLeaseRequest(props.workerId(), props.leaseBatchSize()),
                new ParameterizedTypeReference<ApiResponse<List<TaskLeaseObjectResponse>>>() {});
        if (response == null || response.code() == null || response.code() != 0) {
            return List.of();
        }
        return Objects.requireNonNullElse(response.data(), List.of());
    }

    public void reportDone(Long taskId, String objectKey) {
        report(taskId, objectKey, "done", null);
    }

    public void reportFailed(Long taskId, String objectKey, String errorMessage) {
        report(taskId, objectKey, "failed", errorMessage);
    }

    private void report(Long taskId, String objectKey, String status, String errorMessage) {
        String path = "/api/tasks/%d/report".formatted(taskId);
        post(path,
                new TaskReportRequest(props.workerId(), objectKey, status, errorMessage),
                new ParameterizedTypeReference<ApiResponse<Object>>() {});
    }

    private <T, R> R post(String path, T body, ParameterizedTypeReference<R> type) {
        ResponseEntity<R> entity = restTemplate.exchange(
                props.masterUrl() + path,
                HttpMethod.POST,
                new HttpEntity<>(body),
                type
        );
        return entity.getBody();
    }

    private <R> R get(String path, ParameterizedTypeReference<R> type) {
        ResponseEntity<R> entity = restTemplate.exchange(
                props.masterUrl() + path,
                HttpMethod.GET,
                HttpEntity.EMPTY,
                type
        );
        return entity.getBody();
    }
}
