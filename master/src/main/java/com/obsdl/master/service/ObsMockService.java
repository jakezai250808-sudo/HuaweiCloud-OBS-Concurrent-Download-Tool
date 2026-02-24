package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsDirectoryItemResponse;
import com.obsdl.master.dto.obs.ObsObjectItemResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;
import com.obsdl.master.entity.ObsAccountEntity;
import com.obsdl.master.entity.ObsMockObjectEntity;
import com.obsdl.master.exception.BizException;
import com.obsdl.master.service.crud.ObsAccountCrudService;
import com.obsdl.master.service.crud.ObsMockObjectCrudService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
@ConditionalOnProperty(prefix = "obs.browser", name = "mode", havingValue = "mock", matchIfMissing = true)
public class ObsMockService implements ObsBrowserService {

    private static final String DEFAULT_DELIMITER = "/";
    private static final DateTimeFormatter ISO_8601 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObsAccountCrudService obsAccountCrudService;
    private final ObsMockObjectCrudService obsMockObjectCrudService;

    public ObsMockService(ObsAccountCrudService obsAccountCrudService,
                          ObsMockObjectCrudService obsMockObjectCrudService) {
        this.obsAccountCrudService = obsAccountCrudService;
        this.obsMockObjectCrudService = obsMockObjectCrudService;
    }

    @Override
    public BucketListResponse listBuckets(Long accountId) {
        ObsAccountEntity account = obsAccountCrudService.getById(accountId);
        if (account == null) {
            throw new BizException(40401, "账户不存在");
        }
        List<String> buckets = obsAccountCrudService.lambdaQuery()
                .eq(ObsAccountEntity::getId, accountId)
                .list()
                .stream()
                .map(ObsAccountEntity::getBucket)
                .filter(bucket -> bucket != null && !bucket.isBlank())
                .distinct()
                .sorted()
                .toList();
        return new BucketListResponse(buckets);
    }

    @Override
    public ObsObjectListingResponse listObjects(
            String bucket,
            String prefix,
            String delimiter,
            String marker,
            String continuationToken
    ) {
        String normalizedDelimiter = normalizeDelimiter(delimiter);
        String normalizedPrefix = normalizePrefix(prefix, normalizedDelimiter);

        List<ObsMockObjectEntity> candidates = obsMockObjectCrudService.lambdaQuery()
                .eq(ObsMockObjectEntity::getBucket, bucket)
                .likeRight(ObsMockObjectEntity::getObjectKey, normalizedPrefix)
                .orderByAsc(ObsMockObjectEntity::getObjectKey)
                .list();

        Map<String, ObsDirectoryItemResponse> directories = new TreeMap<>();
        List<ObsObjectItemResponse> objects = new java.util.ArrayList<>();

        for (ObsMockObjectEntity entity : candidates) {
            String key = entity.getObjectKey();
            if (!key.startsWith(normalizedPrefix)) {
                continue;
            }
            String remainder = key.substring(normalizedPrefix.length());
            if (remainder.isEmpty()) {
                continue;
            }

            int nextDelimiterIndex = normalizedDelimiter.isEmpty() ? -1 : remainder.indexOf(normalizedDelimiter);
            if (nextDelimiterIndex >= 0) {
                String name = remainder.substring(0, nextDelimiterIndex);
                if (!name.isEmpty()) {
                    String directoryPrefix = normalizedPrefix
                            + remainder.substring(0, nextDelimiterIndex + normalizedDelimiter.length());
                    directories.putIfAbsent(name, new ObsDirectoryItemResponse(name, directoryPrefix));
                }
                continue;
            }

            String lastModified = entity.getLastModified() == null
                    ? null
                    : entity.getLastModified().atOffset(ZoneOffset.UTC).format(ISO_8601);
            objects.add(new ObsObjectItemResponse(
                    entity.getObjectKey(),
                    entity.getSize() == null ? 0L : entity.getSize(),
                    lastModified,
                    entity.getEtag(),
                    entity.getStorageClass()
            ));
        }

        objects.sort(Comparator.comparing(ObsObjectItemResponse::key));

        return new ObsObjectListingResponse(
                bucket,
                normalizedPrefix,
                normalizedDelimiter,
                List.copyOf(directories.values()),
                objects,
                false,
                null,
                null
        );
    }

    private String normalizeDelimiter(String delimiter) {
        if (delimiter == null || delimiter.isBlank()) {
            return DEFAULT_DELIMITER;
        }
        return delimiter;
    }

    private String normalizePrefix(String prefix, String delimiter) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String trimmed = prefix.trim();
        if (trimmed.equals(delimiter)) {
            return "";
        }
        if (trimmed.endsWith(delimiter)) {
            return trimmed;
        }
        return trimmed + delimiter;
    }
}
