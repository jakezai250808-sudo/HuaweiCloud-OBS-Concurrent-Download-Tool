package com.obsdl.master.service;

import com.obsdl.master.config.ControlProperties;
import com.obsdl.master.dto.ros.RosStartRequest;
import com.obsdl.master.dto.ros.RosStatusResponse;
import com.obsdl.master.dto.ros.RosVersion;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
public class RosControlService {

    private final ControlProperties controlProperties;
    private RosRuntimeState state = RosRuntimeState.stopped("Not started");

    public RosControlService(ControlProperties controlProperties) {
        this.controlProperties = controlProperties;
    }

    public synchronized RosStatusResponse start(RosStartRequest request) {
        RosStatusResponse current = status();
        if ("RUNNING".equals(current.status()) || "STARTING".equals(current.status())) {
            // L1 单会话：重复 start 时先 stop 再 start。
            stop();
        }

        RosRuntimeState next = new RosRuntimeState(
                "STARTING",
                request.rosVersion(),
                request.bagPath(),
                request.port(),
                null,
                null,
                null,
                null
        );

        try {
            if (request.rosVersion() == RosVersion.ROS1) {
                next.roscorePid = spawn(next.rosVersion, buildRos1SourceAndCommand("roscore"));
            }
            next.bridgePid = spawn(next.rosVersion, buildBridgeCommand(request));
            next.bagPid = spawn(next.rosVersion, buildBagCommand(request));
            next.status = "RUNNING";
            state = next;
            return toResponse(state);
        } catch (Exception ex) {
            state = RosRuntimeState.stopped(ex.getMessage());
            return toResponse(state);
        }
    }

    public synchronized RosStatusResponse stop() {
        killGroup(state.bagPid);
        killGroup(state.bridgePid);
        killGroup(state.roscorePid);
        state = RosRuntimeState.stopped(null);
        return toResponse(state);
    }

    public synchronized RosStatusResponse status() {
        if (!"STOPPED".equals(state.status)) {
            boolean alive = isAlive(state.bagPid) && isAlive(state.bridgePid);
            if (state.rosVersion == RosVersion.ROS1) {
                alive = alive && isAlive(state.roscorePid);
            }
            if (!alive) {
                state = RosRuntimeState.stopped("Process not alive");
            }
        }
        return toResponse(state);
    }

    private String buildRos1SourceAndCommand(String command) {
        return "source " + shellQuote(controlProperties.ros1SetupOrDefault()) + " && " + command;
    }

    private String buildRos2SourceAndCommand(String command) {
        return "source " + shellQuote(controlProperties.ros2SetupOrDefault()) + " && " + command;
    }

    private String buildBridgeCommand(RosStartRequest request) {
        if (request.rosVersion() == RosVersion.ROS1) {
            String cmd = "roslaunch rosbridge_server rosbridge_websocket.launch port:=" + request.port();
            return buildRos1SourceAndCommand(cmd);
        }
        String cmd = "ros2 launch rosbridge_server rosbridge_websocket_launch.xml port:=" + request.port();
        return buildRos2SourceAndCommand(cmd);
    }

    private String buildBagCommand(RosStartRequest request) {
        if (request.rosVersion() == RosVersion.ROS1) {
            StringBuilder cmd = new StringBuilder("rosbag play ").append(shellQuote(request.bagPath()));
            if (request.useSimTime()) {
                cmd.append(" --clock");
            }
            if (request.loop()) {
                cmd.append(" -l");
            }
            cmd.append(" -r ").append(request.rate());
            return buildRos1SourceAndCommand(cmd.toString());
        }

        StringBuilder cmd = new StringBuilder("ros2 bag play ").append(shellQuote(request.bagPath()));
        if (request.loop()) {
            cmd.append(" --loop");
        }
        return buildRos2SourceAndCommand(cmd.toString());
    }

    private long spawn(RosVersion rosVersion, String sourcedCommand) throws IOException, InterruptedException {
        String wrapped = "setsid bash -lc " + shellQuote("exec " + sourcedCommand) +
                " >> " + shellQuote(controlProperties.logOutOrDefault()) +
                " 2>> " + shellQuote(controlProperties.logErrOrDefault()) +
                " < /dev/null & echo $!";
        Process process = new ProcessBuilder("bash", "-lc", wrapped).start();
        String pidText;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            pidText = reader.readLine();
        }
        int code = process.waitFor();
        if (code != 0 || pidText == null || pidText.isBlank()) {
            throw new IOException("Failed to start process for " + rosVersion + ", exit=" + code);
        }
        return Long.parseLong(pidText.trim());
    }

    private void killGroup(Long pid) {
        if (pid == null) {
            return;
        }
        runQuietly("kill -TERM -" + pid);
        runQuietly("sleep 0.5");
        runQuietly("kill -KILL -" + pid);
    }

    private boolean isAlive(Long pid) {
        if (pid == null) {
            return false;
        }
        try {
            Process process = new ProcessBuilder("bash", "-lc", "kill -0 " + pid).start();
            return process.waitFor() == 0;
        } catch (Exception ex) {
            return false;
        }
    }

    private void runQuietly(String cmd) {
        try {
            Process process = new ProcessBuilder("bash", "-lc", cmd).start();
            process.waitFor();
        } catch (Exception ignored) {
        }
    }

    private RosStatusResponse toResponse(RosRuntimeState runtimeState) {
        return new RosStatusResponse(
                runtimeState.status,
                runtimeState.rosVersion,
                runtimeState.bagPath,
                runtimeState.port,
                runtimeState.port == null ? null : "ws://" + controlProperties.hostForWsUrlOrDefault() + ":" + runtimeState.port,
                runtimeState.roscorePid,
                runtimeState.bridgePid,
                runtimeState.bagPid,
                runtimeState.message
        );
    }

    private static String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }

    private static final class RosRuntimeState {
        private String status;
        private RosVersion rosVersion;
        private String bagPath;
        private Integer port;
        private Long roscorePid;
        private Long bridgePid;
        private Long bagPid;
        private String message;

        private RosRuntimeState(String status,
                                RosVersion rosVersion,
                                String bagPath,
                                Integer port,
                                Long roscorePid,
                                Long bridgePid,
                                Long bagPid,
                                String message) {
            this.status = status;
            this.rosVersion = rosVersion;
            this.bagPath = bagPath;
            this.port = port;
            this.roscorePid = roscorePid;
            this.bridgePid = bridgePid;
            this.bagPid = bagPid;
            this.message = message;
        }

        private static RosRuntimeState stopped(String message) {
            return new RosRuntimeState("STOPPED", null, null, null, null, null, null, message);
        }
    }
}
