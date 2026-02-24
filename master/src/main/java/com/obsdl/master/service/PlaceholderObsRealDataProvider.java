package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObsObjectListingResponse;
import com.obsdl.master.exception.BizException;
public class PlaceholderObsRealDataProvider implements ObsRealDataProvider {

    private static final String MESSAGE = "OBS real mode is enabled but no SDK-backed ObsRealDataProvider is configured.";

    @Override
    public BucketListResponse listBuckets(Long accountId) {
        throw new BizException(50010, MESSAGE);
    }

    @Override
    public ObsObjectListingResponse listObjects(
            String bucket,
            String prefix,
            String delimiter,
            String marker,
            String continuationToken
    ) {
        throw new BizException(50010, MESSAGE);
    }
}
