package com.obsdl.master.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ControlPropertiesTest {

    @Test
    void shouldUseConfiguredHostForWsUrl() {
        ControlProperties properties = new ControlProperties(null, null, null, null, "10.10.10.12", 60);

        assertEquals("10.10.10.12", properties.hostForWsUrlOrDefault());
    }

    @Test
    void shouldResolveHostWhenConfiguredAsLocalhost() {
        ControlProperties properties = new ControlProperties(null, null, null, null, "localhost", 60);

        assertFalse(properties.hostForWsUrlOrDefault().isBlank());
    }
}
