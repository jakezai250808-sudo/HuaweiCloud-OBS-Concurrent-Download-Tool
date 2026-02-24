package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsDirectoryItemResponse;
import com.obsdl.master.dto.obs.ObsObjectItemResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;
import com.obsdl.master.entity.ObsAccountEntity;
import com.obsdl.master.exception.BizException;
import com.obsdl.master.service.crud.ObsAccountCrudService;
import com.obs.services.ObsClient;
import com.obs.services.exception.ObsException;
import com.obs.services.model.ListObjectsRequest;
import com.obs.services.model.ObjectListing;
import com.obs.services.model.ObsObject;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "obs.browser", name = "mode", havingValue = "real")
public class SdkObsRealDataProvider implements ObsRealDataProvider {

    private static final String DEFAULT_DELIMITER = "/";
    private static final DateTimeFormatter ISO_8601 = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final ObsAccountCrudService obsAccountCrudService;

    public SdkObsRealDataProvider(ObsAccountCrudService obsAccountCrudService) {
        this.obsAccountCrudService = obsAccountCrudService;
    }

    @Override
    public BucketListResponse listBuckets(Long accountId) {
        ObsAccountEntity account = obsAccountCrudService.getById(accountId);
        if (account == null) {
            throw new BizException(40401, "账户不存在");
        }
        Set<String> buckets = new LinkedHashSet<>();
        if (account.getBucket() != null && !account.getBucket().isBlank()) {
            buckets.add(account.getBucket().trim());
        }
        return new BucketListResponse(buckets.stream().sorted().toList());
    }

    @Override
    public ObsObjectListingResponse listObjects(
            String bucket,
            String prefix,
            String delimiter,
            String marker,
            String continuationToken
    ) {
        ObsAccountEntity account = resolveAccountForBucket(bucket);
        String normalizedDelimiter = normalizeDelimiter(delimiter);
        String normalizedPrefix = normalizePrefix(prefix, normalizedDelimiter);
        String effectiveMarker = (marker == null || marker.isBlank()) ? continuationToken : marker;

        ListObjectsRequest request = new ListObjectsRequest(bucket);
        request.setPrefix(normalizedPrefix);
        request.setDelimiter(normalizedDelimiter);
        if (effectiveMarker != null && !effectiveMarker.isBlank()) {
            request.setMarker(effectiveMarker);
        }

        try (ObsClient client = createClient(account)) {
            ObjectListing listing = client.listObjects(request);
            List<ObsDirectoryItemResponse> directories = toDirectories(normalizedPrefix, normalizedDelimiter, listing);
            List<ObsObjectItemResponse> objects = toObjects(listing);
            String nextMarker = listing.isTruncated() ? listing.getNextMarker() : null;
            return new ObsObjectListingResponse(
                    bucket,
                    normalizedPrefix,
                    normalizedDelimiter,
                    directories,
                    objects,
                    listing.isTruncated(),
                    nextMarker,
                    nextMarker
            );
        } catch (ObsException ex) {
            throw new BizException(50012, "OBS list objects failed for bucket " + bucket);
        } catch (Exception ex) {
            throw new BizException(50012, "OBS list objects failed");
        }
    }

    private List<ObsAccountEntity> listAccounts() {
        List<ObsAccountEntity> accounts = obsAccountCrudService.lambdaQuery()
                .orderByAsc(ObsAccountEntity::getId)
                .list();
        if (accounts.isEmpty()) {
            throw new BizException(40003, "No OBS account configured");
        }
        return accounts;
    }

    private ObsAccountEntity resolveAccountForBucket(String bucket) {
        List<ObsAccountEntity> accounts = listAccounts();
        return accounts.stream()
                .filter(account -> bucket.equals(account.getBucket()))
                .findFirst()
                .orElse(accounts.get(0));
    }

    private ObsClient createClient(ObsAccountEntity account) {
        return new ObsClient(account.getAccessKey(), account.getSecretKey(), account.getEndpoint());
    }

    private List<ObsDirectoryItemResponse> toDirectories(String normalizedPrefix, String delimiter, ObjectListing listing) {
        Set<String> prefixes = new LinkedHashSet<>(listing.getCommonPrefixes());
        List<ObsDirectoryItemResponse> directories = new ArrayList<>();
        for (String directoryPrefix : prefixes) {
            if (directoryPrefix == null || directoryPrefix.isBlank()) {
                continue;
            }
            String relative = directoryPrefix.startsWith(normalizedPrefix)
                    ? directoryPrefix.substring(normalizedPrefix.length())
                    : directoryPrefix;
            if (relative.endsWith(delimiter)) {
                relative = relative.substring(0, relative.length() - delimiter.length());
            }
            if (!relative.isBlank()) {
                directories.add(new ObsDirectoryItemResponse(relative, directoryPrefix));
            }
        }
        directories.sort(Comparator.comparing(ObsDirectoryItemResponse::name));
        return directories;
    }

    private List<ObsObjectItemResponse> toObjects(ObjectListing listing) {
        List<ObsObjectItemResponse> objects = new ArrayList<>();
        for (ObsObject object : listing.getObjects()) {
            if (object == null || object.getObjectKey() == null || object.getObjectKey().isBlank()) {
                continue;
            }
            String lastModified = object.getMetadata() != null && object.getMetadata().getLastModified() != null
                    ? object.getMetadata().getLastModified().toInstant().atOffset(ZoneOffset.UTC).format(ISO_8601)
                    : null;
            long size = object.getMetadata() == null ? 0L : object.getMetadata().getContentLength();
            String etag = object.getMetadata() == null ? null : object.getMetadata().getEtag();
            String storageClass = object.getMetadata() == null ? null : object.getMetadata().getStorageClass();
            objects.add(new ObsObjectItemResponse(object.getObjectKey(), size, lastModified, etag, storageClass));
        }
        objects.sort(Comparator.comparing(ObsObjectItemResponse::key));
        return objects;
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
