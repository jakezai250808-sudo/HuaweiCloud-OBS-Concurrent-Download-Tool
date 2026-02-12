package com.obsdl.worker.obs;

import com.obsdl.worker.dto.TaskLeaseObjectResponse;

import java.nio.file.Path;

public interface ObsClientFacade {

    void downloadToFile(TaskLeaseObjectResponse leaseObject, Path destination) throws Exception;
}
