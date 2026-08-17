package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ReadFileService {

    private static final Logger log =
            LoggerFactory.getLogger(ReadFileService.class);

    private final ProjectRegistry projectRegistry;

    public ReadFileService(ProjectRegistry projectRegistry) {
        this.projectRegistry = Objects.requireNonNull(
                projectRegistry,
                "projectRegistry must not be null"
        );
    }

    public String read(String projectName, Path filePath) throws IOException {
        Objects.requireNonNull(projectName, "projectName must not be null");
        Objects.requireNonNull(filePath, "filePath must not be null");

        log.debug(
                "Starting file read: projectName={}, filePath={}",
                projectName,
                filePath
        );

        Path rootPath = projectRegistry.getRoot(projectName);

        if (!filePath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "file path is outside registered project root: " + filePath
            );
        }

        Path realPath = filePath.toRealPath();

        if (!realPath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "real file path is outside registered project root: " + realPath
            );
        }

        if (!Files.isRegularFile(realPath)) {
            throw new IllegalArgumentException(
                    "file path must point to a regular file: " + realPath
            );
        }

        if (!Files.isReadable(realPath)) {
            throw new IllegalArgumentException(
                    "file is not readable: " + realPath
            );
        }

        String content = Files.readString(realPath);

        log.debug(
                "File read completed: projectName={}, filePath={}",
                projectName,
                realPath
        );

        return content;
    }
}