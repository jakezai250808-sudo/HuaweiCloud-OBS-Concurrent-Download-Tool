package com.obsdl.master.service;

import com.obsdl.master.dto.obs.BucketListResponse;
import com.obsdl.master.dto.obs.ObjectListResponse;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ObsMockService {

    private static final Map<String, List<String>> MOCK_OBJECTS_BY_BUCKET = buildMockData();

    public BucketListResponse listBuckets() {
        return new BucketListResponse(List.copyOf(MOCK_OBJECTS_BY_BUCKET.keySet()));
    }

    public ObjectListResponse listObjects(String bucket) {
        List<String> objectKeys = MOCK_OBJECTS_BY_BUCKET.get(bucket);
        if (objectKeys == null) {
            objectKeys = List.of(
                    "incoming/" + bucket + "/2026/01/summary.csv",
                    "incoming/" + bucket + "/2026/01/events-0001.json",
                    "incoming/" + bucket + "/2026/01/events-0002.json",
                    "incoming/" + bucket + "/readme.txt"
            );
        }
        return new ObjectListResponse(bucket, objectKeys);
    }

    private static Map<String, List<String>> buildMockData() {
        Map<String, List<String>> mockData = new LinkedHashMap<>();
        mockData.put("demo-bucket-a", List.of(
                "finance/2025/12/report-20251201.csv",
                "finance/2025/12/report-20251202.csv",
                "finance/2025/12/report-20251203.csv",
                "finance/2026/01/report-20260101.csv",
                "finance/2026/01/report-20260102.csv",
                "finance/archive/readme.md"
        ));
        mockData.put("demo-bucket-b", List.of(
                "media/images/banner-home-v1.png",
                "media/images/banner-home-v2.png",
                "media/images/logo.svg",
                "media/videos/intro-1080p.mp4",
                "media/videos/intro-4k.mp4",
                "media/thumbs/intro-0001.jpg",
                "media/thumbs/intro-0002.jpg"
        ));
        mockData.put("demo-bucket-c", List.of(
                "warehouse/orders/2026/02/partition=01/part-0001.parquet",
                "warehouse/orders/2026/02/partition=01/part-0002.parquet",
                "warehouse/orders/2026/02/partition=02/part-0001.parquet",
                "warehouse/orders/2026/02/partition=02/part-0002.parquet",
                "warehouse/users/snapshot-20260201.orc"
        ));
        mockData.put("demo-bucket-log", List.of(
                "app-logs/worker-a/2026-02-18.log.gz",
                "app-logs/worker-a/2026-02-19.log.gz",
                "app-logs/worker-b/2026-02-19.log.gz",
                "app-logs/master/2026-02-19.log.gz",
                "app-logs/master/2026-02-20.log.gz"
        ));
        mockData.put("demo-bucket-backup", List.of(
                "backup/mysql/full-2026-02-15.sql.gz",
                "backup/mysql/inc-2026-02-16.sql.gz",
                "backup/mysql/inc-2026-02-17.sql.gz",
                "backup/config/master-2026-02-20.tar.gz",
                "backup/config/worker-2026-02-20.tar.gz",
                "backup/restore-guide.txt"
        ));
        return Map.copyOf(mockData);
    }
}
