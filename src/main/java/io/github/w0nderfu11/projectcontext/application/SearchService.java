package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public final class SearchService {

    private static final Logger log =
            LoggerFactory.getLogger(SearchService.class);

    private final ProjectRegistry projectRegistry;

    public SearchService(ProjectRegistry projectRegistry) {
        this.projectRegistry = Objects.requireNonNull(
                projectRegistry,
                "projectRegistry must not be null"
        );
    }

    public List<String> search(
            String projectName,
            String fileName,
            String extension,
            Path directoryPath
    ) throws IOException {
        Objects.requireNonNull(projectName, "projectName must not be null");
        Objects.requireNonNull(fileName, "fileName must not be null");
        Objects.requireNonNull(extension, "extension must not be null");

        if (fileName.isBlank()) {
            throw new IllegalArgumentException(
                    "fileName must not be blank"
            );
        }

        if (extension.isBlank()) {
            throw new IllegalArgumentException(
                    "extension must not be blank"
            );
        }

        log.debug(
                "Starting file search: projectName={}, fileName={}, extension={}, directoryPath={}",
                projectName,
                fileName,
                extension,
                directoryPath
        );

        Path rootPath = projectRegistry.getRoot(projectName);
        Path searchPath = rootPath;

        if (directoryPath != null
                && !directoryPath.startsWith(rootPath)) {
            throw new IllegalArgumentException(
                    "directory path is outside registered project root: " + directoryPath
            );
        }

        if (directoryPath != null) {
            Path realDirectoryPath = directoryPath.toRealPath();

            if (!realDirectoryPath.startsWith(rootPath)) {
                throw new IllegalArgumentException(
                        "real directory path is outside registered project root: "
                                + realDirectoryPath
                );
            }

            if (!Files.isDirectory(realDirectoryPath)) {
                throw new IllegalArgumentException(
                        "directory path must point to a directory: "
                                + realDirectoryPath
                );
            }

            searchPath = realDirectoryPath;
        }

        String normalizedFileName =
                fileName.toLowerCase(Locale.ROOT);

        String normalizedExtension =
                extension.toLowerCase(Locale.ROOT);

        Set<Path> matches = new HashSet<>();

        try (Stream<Path> paths = Files.walk(searchPath)) {
            paths.forEach(path -> {
                try {
                    Path realPath = path.toRealPath();

                    if (!realPath.startsWith(rootPath)) {
                        return;
                    }

                    if (!Files.isRegularFile(realPath)) {
                        return;
                    }

                    String name = realPath.getFileName()
                            .toString()
                            .toLowerCase(Locale.ROOT);

                    if (!name.endsWith("." + normalizedExtension)) {
                        return;
                    }

                    String nameWithoutExtension = name.substring(
                            0,
                            name.length() - normalizedExtension.length() - 1
                    );

                    if (!nameWithoutExtension.contains(normalizedFileName)) {
                        return;
                    }

                    matches.add(realPath);
                } catch (IOException e) {
                    log.debug(
                            "Skipping file during search: filePath={}",
                            path
                    );
                }
            });
        }

        List<String> result = matches.stream()
                .map(Path::toString)
                .toList();

        log.debug(
                "File search completed: projectName={}, matches={}",
                projectName,
                result.size()
        );

        return result;
    }
}