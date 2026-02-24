package com.obsdl.worker.obs;

import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.GetObjectRequest;
import com.obs.services.model.ObsObject;
import com.obsdl.worker.dto.TaskLeaseObjectResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Slf4j
@Component
public class SdkObsClientFacade implements ObsClientFacade {

    @Override
    public void downloadToFile(TaskLeaseObjectResponse leaseObject, Path destination) throws Exception {
        validateLeaseObject(leaseObject);
        Files.createDirectories(destination.getParent());

        try (ObsClient client = new ObsClient(leaseObject.ak(), leaseObject.sk(), leaseObject.endpoint())) {
            GetObjectRequest request = new GetObjectRequest(leaseObject.bucket(), leaseObject.objectKey());
            ObsObject object = client.getObject(request);
            try (InputStream input = object.getObjectContent();
                 OutputStream output = Files.newOutputStream(
                         destination,
                         StandardOpenOption.CREATE,
                         StandardOpenOption.TRUNCATE_EXISTING,
                         StandardOpenOption.WRITE
                 )) {
                input.transferTo(output);
            }
        } catch (ObsException ex) {
            throw new IllegalStateException(
                    "OBS download failed, key=%s, status=%d, code=%s".formatted(
                            leaseObject.objectKey(),
                            ex.getResponseCode(),
                            ex.getErrorCode()
                    ),
                    ex
            );
        }

        verifyDownloadedFile(leaseObject, destination);
    }

    private void validateLeaseObject(TaskLeaseObjectResponse leaseObject) {
        requireNotBlank(leaseObject.endpoint(), "endpoint");
        requireNotBlank(leaseObject.ak(), "ak");
        requireNotBlank(leaseObject.sk(), "sk");
        requireNotBlank(leaseObject.bucket(), "bucket");
        requireNotBlank(leaseObject.objectKey(), "objectKey");
    }

    private void verifyDownloadedFile(TaskLeaseObjectResponse leaseObject, Path destination) throws Exception {
        if (leaseObject.size() != null && leaseObject.size() > 0) {
            long downloadedSize = Files.size(destination);
            if (downloadedSize != leaseObject.size()) {
                throw new IllegalStateException(
                        "download size mismatch, key=%s, expected=%d, actual=%d".formatted(
                                leaseObject.objectKey(),
                                leaseObject.size(),
                                downloadedSize
                        )
                );
            }
        }
    }

    private void requireNotBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank");
        }
    }
}
