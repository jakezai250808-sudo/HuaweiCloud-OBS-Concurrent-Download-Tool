package com.obsdl.master.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obsdl.master.config.ControlProperties;
import com.obsdl.master.entity.ApiTokenEntity;
import com.obsdl.master.mapper.ApiTokenMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class ApiTokenService {

    private final ApiTokenMapper apiTokenMapper;
    private final ControlProperties controlProperties;
    private final Set<String> tokenCache = ConcurrentHashMap.newKeySet();
    private volatile long lastLoadedAt = 0L;

    public ApiTokenService(ApiTokenMapper apiTokenMapper, ControlProperties controlProperties) {
        this.apiTokenMapper = apiTokenMapper;
        this.controlProperties = controlProperties;
    }

    public boolean isTokenValid(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }
        reloadTokensIfNeeded();
        return tokenCache.contains(token);
    }

    public synchronized void reloadTokensIfNeeded() {
        long now = Instant.now().toEpochMilli();
        long ttlMs = controlProperties.tokenCacheTtlSecondsOrDefault() * 1000;
        if (now - lastLoadedAt < ttlMs && !tokenCache.isEmpty()) {
            return;
        }

        tokenCache.clear();
        try {
            LambdaQueryWrapper<ApiTokenEntity> query = new LambdaQueryWrapper<ApiTokenEntity>()
                    .eq(ApiTokenEntity::getEnabled, true)
                    .isNotNull(ApiTokenEntity::getToken);
            apiTokenMapper.selectList(query).stream()
                    .map(ApiTokenEntity::getToken)
                    .filter(v -> v != null && !v.isBlank())
                    .forEach(tokenCache::add);
        } catch (Exception ex) {
            // 降级策略：表不存在/数据库异常时，不放行任何 token，避免接口 500。
            log.warn("failed to load api tokens from DB, all ROS control requests will be unauthorized", ex);
        } finally {
            lastLoadedAt = now;
        }
    }
}
