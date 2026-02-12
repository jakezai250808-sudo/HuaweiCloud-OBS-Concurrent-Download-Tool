package com.obsdl.master.dto.obs;

import java.util.List;

public record ObjectListResponse(String bucket, List<String> objectKeys) {
}
