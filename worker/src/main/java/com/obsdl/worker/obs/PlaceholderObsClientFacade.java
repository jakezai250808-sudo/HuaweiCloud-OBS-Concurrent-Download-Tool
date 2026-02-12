package com.obsdl.worker.obs;

import com.obsdl.worker.dto.TaskLeaseObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class PlaceholderObsClientFacade implements ObsClientFacade {

    @Override
    public void downloadToFile(TaskLeaseObjectResponse leaseObject, Path destination) throws Exception {
        Files.createDirectories(destination.getParent());
        throw new UnsupportedOperationException(
                "OBS Java SDK adapter not configured. Please replace PlaceholderObsClientFacade with SDK-backed implementation.");
    }
}
