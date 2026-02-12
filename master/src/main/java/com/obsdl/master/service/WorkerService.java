package com.obsdl.master.service;

import com.obsdl.master.dto.worker.WorkerHeartbeatRequest;
import com.obsdl.master.dto.worker.WorkerRegisterRequest;
import com.obsdl.master.dto.worker.WorkerResponse;
import com.obsdl.master.exception.BizException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkerService {

    private final ConcurrentHashMap<String, WorkerResponse> workers = new ConcurrentHashMap<>();

    public WorkerResponse register(WorkerRegisterRequest request) {
        WorkerResponse worker = new WorkerResponse(
                request.workerId(),
                request.host(),
                request.port(),
                request.maxConcurrency(),
                0,
                0,
                Instant.now()
        );
        workers.put(request.workerId(), worker);
        return worker;
    }

    public WorkerResponse heartbeat(WorkerHeartbeatRequest request) {
        WorkerResponse existing = workers.get(request.workerId());
        if (existing == null) {
            throw new BizException(40402, "Worker 未注册");
        }
        WorkerResponse updated = new WorkerResponse(
                existing.workerId(),
                existing.host(),
                existing.port(),
                existing.maxConcurrency(),
                request.activeTasks(),
                request.queuedTasks(),
                Instant.now()
        );
        workers.put(request.workerId(), updated);
        return updated;
    }
}
