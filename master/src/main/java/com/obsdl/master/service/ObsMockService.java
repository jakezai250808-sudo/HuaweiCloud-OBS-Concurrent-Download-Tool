package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObjectListResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ObsMockService {

    public BucketListResponse listBuckets() {
        return new BucketListResponse(List.of("demo-bucket-a", "demo-bucket-b", "demo-bucket-c"));
    }

    public ObjectListResponse listObjects(String bucket) {
        return new ObjectListResponse(bucket, List.of("mock/path/file-1.txt", "mock/path/file-2.txt", "mock/path/file-3.txt"));
    }
}
