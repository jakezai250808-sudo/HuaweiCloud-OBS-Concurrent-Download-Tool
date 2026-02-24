package com.obsdl.master.dto.obs;

import java.util.List;

@Deprecated
public record ObjectListResponse(String bucket, List<String> objectKeys) {
}
