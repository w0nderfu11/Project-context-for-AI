package io.github.w0nderfu11.projectcontext.registry;

import java.nio.file.Path;

public record ProjectArtifact(
        String name,
        Path rootPath
) {
}