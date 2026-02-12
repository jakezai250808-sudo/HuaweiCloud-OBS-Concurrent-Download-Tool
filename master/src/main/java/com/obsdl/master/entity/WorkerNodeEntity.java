package com.obsdl.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("worker_node")
public class WorkerNodeEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("node_name")
    private String nodeName;

    private String host;

    private Integer port;

    private String status;

    @TableField("last_heartbeat")
    private LocalDateTime lastHeartbeat;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
