package com.obsdl.common.dto;

import com.obsdl.common.enums.TaskStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskResponse {
    private Long taskId;
    private String bucket;
    private String objectKey;
    private Integer concurrency;
    private TaskStatus status;
}
