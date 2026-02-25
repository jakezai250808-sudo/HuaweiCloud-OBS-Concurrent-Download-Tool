package com.obsdl.master.controller;

import com.obsdl.master.dto.ros.RosStartRequest;
import com.obsdl.master.dto.ros.RosStatusResponse;
import com.obsdl.master.dto.ros.RosVersion;
import com.obsdl.master.service.ApiTokenService;
import com.obsdl.master.service.RosControlService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RosControlControllerApiTest extends ApiIntegrationTestSupport {

    @MockBean
    private RosControlService rosControlService;

    @MockBean
    private ApiTokenService apiTokenService;

    @Test
    void statusShouldReturn401WhenTokenMissing() throws Exception {
        when(apiTokenService.isTokenValid(null)).thenReturn(false);

        mockMvc.perform(get("/api/v1/ros/status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void statusShouldReturn401WhenTokenNotInDb() throws Exception {
        when(apiTokenService.isTokenValid("invalid")).thenReturn(false);

        mockMvc.perform(get("/api/v1/ros/status")
                        .header("X-CTRL-TOKEN", "invalid")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
    }

    @Test
    void startShouldPassWhenTokenInDb() throws Exception {
        when(apiTokenService.isTokenValid("change_me")).thenReturn(true);

        RosStatusResponse running = new RosStatusResponse(
                "RUNNING",
                RosVersion.ROS1,
                "/tmp/demo.bag",
                9090,
                "ws://localhost:9090",
                11L,
                22L,
                33L,
                null
        );
        when(rosControlService.start(any())).thenReturn(running);

        RosStartRequest request = new RosStartRequest(RosVersion.ROS1, "/tmp/demo.bag", true, true, 1.0, 9090);

        mockMvc.perform(post("/api/v1/ros/start")
                        .header("X-CTRL-TOKEN", "change_me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("RUNNING"))
                .andExpect(jsonPath("$.data.wsUrl").value("ws://localhost:9090"));

        verify(rosControlService).start(any());
    }
}
