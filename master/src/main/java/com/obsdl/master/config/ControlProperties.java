package com.obsdl.master.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "control")
public record ControlProperties(
        String ros1Setup,
        String ros2Setup,
        String logOut,
        String logErr,
        String hostForWsUrl,
        long tokenCacheTtlSeconds
) {
    public String ros1SetupOrDefault() {
        return (ros1Setup == null || ros1Setup.isBlank()) ? "/opt/ros/noetic/setup.bash" : ros1Setup;
    }

    public String ros2SetupOrDefault() {
        return (ros2Setup == null || ros2Setup.isBlank()) ? "/opt/ros/humble/setup.bash" : ros2Setup;
    }

    public String logOutOrDefault() {
        return (logOut == null || logOut.isBlank()) ? "/tmp/rosctl.out" : logOut;
    }

    public String logErrOrDefault() {
        return (logErr == null || logErr.isBlank()) ? "/tmp/rosctl.err" : logErr;
    }

    public String hostForWsUrlOrDefault() {
        return (hostForWsUrl == null || hostForWsUrl.isBlank()) ? "localhost" : hostForWsUrl;
    }

    public long tokenCacheTtlSecondsOrDefault() {
        return tokenCacheTtlSeconds <= 0 ? 60 : tokenCacheTtlSeconds;
    }
}
