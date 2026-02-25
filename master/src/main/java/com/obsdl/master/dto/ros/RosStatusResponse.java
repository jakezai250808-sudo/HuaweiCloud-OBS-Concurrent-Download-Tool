package com.obsdl.master.dto.ros;

public record RosStatusResponse(
        String status,
        RosVersion rosVersion,
        String bagPath,
        Integer port,
        String wsUrl,
        Long roscorePid,
        Long bridgePid,
        Long bagPid,
        String message
) {
}
