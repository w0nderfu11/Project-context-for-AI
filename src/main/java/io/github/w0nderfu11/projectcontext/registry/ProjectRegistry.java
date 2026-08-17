package io.github.w0nderfu11.projectcontext.registry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class ProjectRegistry {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectRegistry.class);

    private final Map<String, ProjectArtifact> artifacts;

    public ProjectRegistry(Map<String, Path> projects) throws IOException {
        Objects.requireNonNull(projects, "projects must not be null");

        if (projects.isEmpty()) {
            throw new IllegalArgumentException(
                    "projects must not be empty"
            );
        }

        log.debug(
                "Starting project registry initialization with {} project(s)",
                projects.size()
        );

        Map<String, ProjectArtifact> registeredArtifacts = new HashMap<>();

        for (Map.Entry<String, Path> entry : projects.entrySet()) {
            String name = entry.getKey();
            Path path = entry.getValue();

            Objects.requireNonNull(name, "project name must not be null");
            Objects.requireNonNull(path, "project path must not be null");

            if (name.isBlank()) {
                throw new IllegalArgumentException(
                        "project name must not be blank"
                );
            }

            if (!path.isAbsolute()) {
                throw new IllegalArgumentException(
                        "project path must be absolute: " + path
                );
            }

            if (!Files.exists(path)) {
                throw new IllegalArgumentException(
                        "project path does not exist: " + path
                );
            }

            if (!Files.isDirectory(path)) {
                throw new IllegalArgumentException(
                        "project path must be a directory: " + path
                );
            }

            Path realPath = path.toRealPath();

            registeredArtifacts.put(
                    name,
                    new ProjectArtifact(name, realPath)
            );

            log.debug(
                    "Registered project: name={}, rootPath={}",
                    name,
                    realPath
            );
        }

        this.artifacts = Map.copyOf(registeredArtifacts);

        log.info(
                "Project registry initialized with {} project(s)",
                artifacts.size()
        );
    }

    public Path getRoot(String name) {
        Objects.requireNonNull(name, "project name must not be null");

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "project name must not be blank"
            );
        }

        ProjectArtifact artifact = artifacts.get(name);

        if (artifact == null) {
            throw new IllegalArgumentException(
                    "project is not registered: " + name
            );
        }

        return artifact.rootPath();
    }
}