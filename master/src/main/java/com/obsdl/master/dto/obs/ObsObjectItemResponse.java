package com.obsdl.master.dto.obs;

public record ObsObjectItemResponse(
        String key,
        long size,
        String lastModified,
        String etag,
        String storageClass
) {
}
