package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

public final class GetCurrentTreeService {

    private static final Logger log =
            LoggerFactory.getLogger(GetCurrentTreeService.class);

    private final ProjectRegistry projectRegistry;

    public GetCurrentTreeService(ProjectRegistry projectRegistry) {
        this.projectRegistry = Objects.requireNonNull(
                projectRegistry,
                "projectRegistry must not be null"
        );
    }

    public Map<String, TreeEntryType> get(
            String projectName,
            Path directoryPath
    ) throws IOException {
        Objects.requireNonNull(projectName, "projectName must not be null");
        Objects.requireNonNull(directoryPath, "directoryPath must not be null");

        log.debug(
                "Starting current tree retrieval: projectName={}, directoryPath={}",
                projectName,
                directoryPath
        );

        Path rootPath = projectRegistry.getRoot(projectName);

        if (!directoryPath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "directory path is outside registered project root: " + directoryPath
            );
        }

        Path realPath = directoryPath.toRealPath();

        if (!realPath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "real directory path is outside registered project root: " + realPath
            );
        }

        if (!Files.isDirectory(realPath)) {
            throw new IllegalArgumentException(
                    "directory path must point to a directory: " + realPath
            );
        }

        Map<String, TreeEntryType> entries = new HashMap<>();

        try (Stream<Path> paths = Files.list(realPath)) {
            paths.forEach(path ->
                    entries.put(
                            path.toString(),
                            Files.isDirectory(path)
                                    ? TreeEntryType.DIRECTORY
                                    : TreeEntryType.FILE
                    )
            );
        }

        log.debug(
                "Current tree retrieval completed: projectName={}, directoryPath={}",
                projectName,
                realPath
        );

        return entries;
    }
}