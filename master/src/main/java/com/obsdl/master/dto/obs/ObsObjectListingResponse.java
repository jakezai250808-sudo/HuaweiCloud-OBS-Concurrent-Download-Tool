package com.obsdl.master.dto.obs;

import java.util.List;

public record ObsObjectListingResponse(
        String bucket,
        String prefix,
        String delimiter,
        List<ObsDirectoryItemResponse> directories,
        List<ObsObjectItemResponse> objects,
        Boolean isTruncated,
        String nextMarker,
        String nextContinuationToken
) {
}
