package com.obsdl.master.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("obs_account")
public class ObsAccountEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("account_name")
    private String accountName;

    @TableField("access_key")
    private String accessKey;

    @TableField("secret_key")
    private String secretKey;

    private String endpoint;

    private String bucket;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
