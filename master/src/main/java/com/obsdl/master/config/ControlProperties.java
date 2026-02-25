package com.obsdl.master.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

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
        if (hostForWsUrl == null || hostForWsUrl.isBlank() || "localhost".equalsIgnoreCase(hostForWsUrl)
                || "127.0.0.1".equals(hostForWsUrl)) {
            return resolveMachineIpOrLocalhost();
        }
        return hostForWsUrl;
    }

    public long tokenCacheTtlSecondsOrDefault() {
        return tokenCacheTtlSeconds <= 0 ? 60 : tokenCacheTtlSeconds;
    }

    private static String resolveMachineIpOrLocalhost() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        return address.getHostAddress();
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception ignored) {
            return "localhost";
        }
    }
}
