package com.obsdl.master.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.obsdl.master.config.ControlProperties;
import com.obsdl.master.entity.ApiTokenEntity;
import com.obsdl.master.mapper.ApiTokenMapper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
        LambdaQueryWrapper<ApiTokenEntity> query = new LambdaQueryWrapper<ApiTokenEntity>()
                .eq(ApiTokenEntity::getEnabled, true)
                .isNotNull(ApiTokenEntity::getToken);
        apiTokenMapper.selectList(query).stream()
                .map(ApiTokenEntity::getToken)
                .filter(v -> v != null && !v.isBlank())
                .forEach(tokenCache::add);
        lastLoadedAt = now;
    }
}
