package com.obsdl.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("download_task")
public class DownloadTaskEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bucket;

    @TableField("object_key")
    private String objectKey;

    private Integer concurrency;

    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
