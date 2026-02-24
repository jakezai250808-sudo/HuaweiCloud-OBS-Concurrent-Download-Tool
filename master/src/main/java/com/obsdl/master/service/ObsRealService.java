package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "obs.browser", name = "mode", havingValue = "real")
public class ObsRealService implements ObsBrowserService {

    private final ObsRealDataProvider obsRealDataProvider;

    public ObsRealService(ObsRealDataProvider obsRealDataProvider) {
        this.obsRealDataProvider = obsRealDataProvider;
    }

    @Override
    public BucketListResponse listBuckets(Long accountId) {
        return obsRealDataProvider.listBuckets(accountId);
    }

    @Override
    public ObsObjectListingResponse listObjects(
            String bucket,
            String prefix,
            String delimiter,
            String marker,
            String continuationToken
    ) {
        return obsRealDataProvider.listObjects(bucket, prefix, delimiter, marker, continuationToken);
    }
}
