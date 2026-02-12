package com.obsdl.worker.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
public class RsyncService {

    public void sync(Path localFile, String remoteDestination) throws Exception {
        List<String> command = List.of("rsync", "-av", "--mkpath", localFile.toString(), remoteDestination);
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
        log.info("rsync stdout:\n{}", stdout);
        if (!stderr.isBlank()) {
            log.warn("rsync stderr:\n{}", stderr);
        }

        if (exit != 0) {
            throw new IllegalStateException("rsync failed with exit code=" + exit + ", stderr=" + stderr);
        }
    }
}
