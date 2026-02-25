package com.obsdl.master.service;

import com.obsdl.master.config.ControlProperties;
import com.obsdl.master.entity.ApiTokenEntity;
import com.obsdl.master.mapper.ApiTokenMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiTokenServiceTest {

    @Mock
    private ApiTokenMapper apiTokenMapper;

    @Test
    void shouldUseTtlCacheToReduceDbQueries() throws Exception {
        ApiTokenEntity tokenEntity = new ApiTokenEntity();
        tokenEntity.setToken("cached-token");
        tokenEntity.setEnabled(true);
        when(apiTokenMapper.selectList(any())).thenReturn(List.of(tokenEntity));

        ApiTokenService service = new ApiTokenService(
                apiTokenMapper,
                new ControlProperties(null, null, null, null, null, 1)
        );

        assertTrue(service.isTokenValid("cached-token"));
        assertTrue(service.isTokenValid("cached-token"));
        assertFalse(service.isTokenValid("other"));

        verify(apiTokenMapper, times(1)).selectList(any());

        Thread.sleep(1100L);
        assertTrue(service.isTokenValid("cached-token"));
        verify(apiTokenMapper, times(2)).selectList(any());
    }
    @Test
    void shouldDenyAllTokensWhenTokenTableMissing() {
        when(apiTokenMapper.selectList(any())).thenThrow(new RuntimeException("Table \"api_token\" not found"));

        ApiTokenService service = new ApiTokenService(
                apiTokenMapper,
                new ControlProperties(null, null, null, null, null, 60)
        );

        assertDoesNotThrow(service::reloadTokensIfNeeded);
        assertFalse(service.isTokenValid("any-token"));
    }

}
