package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;

public interface ObsBrowserService {

    BucketListResponse listBuckets(Long accountId);

    ObsObjectListingResponse listObjects(
            String bucket,
            String prefix,
            String delimiter,
            String marker,
            String continuationToken
    );
}
