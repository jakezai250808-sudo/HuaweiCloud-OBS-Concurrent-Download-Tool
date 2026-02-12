package com.obsdl.worker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "worker.runtime")
public record WorkerRuntimeProperties(
        String masterUrl,
        String workerId,
        Integer maxDlConcurrency,
        String cacheDir,
        String masterSsh,
        String targetPath,
        String targetTmpSubdir,
        Long leaseTaskId,
        Integer heartbeatSeconds,
        Integer leaseBatchSize,
        Integer leaseIdleSleepMs
) {
    public WorkerRuntimeProperties {
        if (leaseTaskId == null) {
            leaseTaskId = 1L;
        }
        if (heartbeatSeconds == null) {
            heartbeatSeconds = 10;
        }
        if (leaseBatchSize == null) {
            leaseBatchSize = 50;
        }
        if (leaseIdleSleepMs == null) {
            leaseIdleSleepMs = 2000;
        }
    }
}
