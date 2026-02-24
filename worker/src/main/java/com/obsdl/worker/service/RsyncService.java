package com.obsdl.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class RsyncService {

    public void sync(Path localFile, String remoteDestination) throws Exception {
        ProcessResult firstAttempt = run(List.of("rsync", "-av", "--mkpath", localFile.toString(), remoteDestination));
        if (firstAttempt.exitCode == 0) {
            return;
        }
        if (supportsMkpathFailed(firstAttempt.stderr)) {
            log.warn("rsync --mkpath unsupported, fallback to ssh mkdir + rsync");
            ensureRemoteDirectoryExists(remoteDestination);
            ProcessResult fallbackAttempt = run(List.of("rsync", "-av", localFile.toString(), remoteDestination));
            if (fallbackAttempt.exitCode == 0) {
                return;
            }
            throw new IllegalStateException("rsync fallback failed with exit code="
                    + fallbackAttempt.exitCode + ", stderr=" + fallbackAttempt.stderr);
        }
        throw new IllegalStateException("rsync failed with exit code="
                + firstAttempt.exitCode + ", stderr=" + firstAttempt.stderr);
    }

    private boolean supportsMkpathFailed(String stderr) {
        return stderr != null && stderr.contains("unrecognized option") && stderr.contains("--mkpath");
    }

    private void ensureRemoteDirectoryExists(String remoteDestination) throws Exception {
        int sep = remoteDestination.indexOf(':');
        if (sep <= 0 || sep >= remoteDestination.length() - 1) {
            throw new IllegalArgumentException("invalid remote destination: " + remoteDestination);
        }
        String remoteHost = remoteDestination.substring(0, sep);
        String remotePath = remoteDestination.substring(sep + 1);
        int slash = remotePath.lastIndexOf('/');
        if (slash <= 0) {
            return;
        }
        String dir = remotePath.substring(0, slash);
        String escapedDir = dir.replace("'", "'\"'\"'");
        List<String> command = new ArrayList<>();
        command.add("ssh");
        command.add(remoteHost);
        command.add("mkdir -p '" + escapedDir + "'");
        ProcessResult mkdirResult = run(command);
        if (mkdirResult.exitCode != 0) {
            throw new IllegalStateException("remote mkdir failed with exit code="
                    + mkdirResult.exitCode + ", stderr=" + mkdirResult.stderr);
        }
    }

    private ProcessResult run(List<String> command) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        Process process = processBuilder.start();

        String stdout;
        try (BufferedReader outReader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            stdout = outReader.lines().reduce("", (acc, line) -> acc + line + System.lineSeparator());
        }

        String stderr;
        try (BufferedReader errReader = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            stderr = errReader.lines().reduce("", (acc, line) -> acc + line + System.lineSeparator());
        }

        int exit = process.waitFor();
        log.info("{} stdout:\n{}", command.get(0), stdout);
        if (!stderr.isBlank()) {
            log.warn("{} stderr:\n{}", command.get(0), stderr);
        }
        return new ProcessResult(exit, stdout, stderr);
    }

    private record ProcessResult(int exitCode, String stdout, String stderr) {
    }
}
