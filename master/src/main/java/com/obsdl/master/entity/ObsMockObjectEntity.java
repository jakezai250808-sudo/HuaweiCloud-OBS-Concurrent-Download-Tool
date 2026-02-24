package com.obsdl.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("obs_mock_object")
public class ObsMockObjectEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bucket;

    @TableField("object_key")
    private String objectKey;

    private Long size;

    @TableField("last_modified")
    private LocalDateTime lastModified;

    private String etag;

    @TableField("storage_class")
    private String storageClass;
}
