package com.obsdl.master.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

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
            List<String> candidates = new ArrayList<>();
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces != null && interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (!isInterfaceEligible(networkInterface)) {
                    continue;
                }
                Enumeration<InetAddress> addresses = networkInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                        candidates.add(address.getHostAddress());
                    }
                }
            }

            if (!candidates.isEmpty()) {
                return candidates.stream()
                        .max((left, right) -> Integer.compare(ipPriority(left), ipPriority(right)))
                        .orElse(candidates.get(0));
            }

            String fallback = InetAddress.getLocalHost().getHostAddress();
            return fallback == null || fallback.isBlank() ? "localhost" : fallback;
        } catch (Exception ignored) {
            return "localhost";
        }
    }

    private static boolean isInterfaceEligible(NetworkInterface networkInterface) {
        try {
            if (!networkInterface.isUp() || networkInterface.isLoopback() || networkInterface.isVirtual()) {
                return false;
            }
            String name = networkInterface.getName();
            if (name == null) {
                return true;
            }
            String normalized = name.toLowerCase(Locale.ROOT);
            return !normalized.startsWith("docker")
                    && !normalized.startsWith("veth")
                    && !normalized.startsWith("br-")
                    && !normalized.startsWith("cni")
                    && !normalized.startsWith("flannel")
                    && !normalized.startsWith("lo");
        } catch (Exception ignored) {
            return false;
        }
    }

    static int ipPriority(String ip) {
        if (ip == null || ip.isBlank()) {
            return 0;
        }
        if (ip.startsWith("10.")) {
            return 400;
        }
        if (ip.startsWith("192.168.")) {
            return 300;
        }
        if (isPrivate172(ip)) {
            return 200;
        }
        return 100;
    }

    private static boolean isPrivate172(String ip) {
        if (!ip.startsWith("172.")) {
            return false;
        }
        String[] parts = ip.split("\\.");
        if (parts.length < 2) {
            return false;
        }
        try {
            int second = Integer.parseInt(parts[1]);
            return second >= 16 && second <= 31;
        } catch (NumberFormatException ex) {
            return false;
        }
    }
}
