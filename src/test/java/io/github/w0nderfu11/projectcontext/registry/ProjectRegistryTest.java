package io.github.w0nderfu11.projectcontext.registry;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectRegistryTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldRegisterProjectAndReturnRealRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", project)
        );

        assertEquals(
                project.toRealPath(),
                registry.getRoot("project")
        );
    }

    @Test
    void shouldRegisterMultipleProjects() throws IOException {
        Path firstProject = Files.createDirectory(
                tempDir.resolve("first-project")
        );
        Path secondProject = Files.createDirectory(
                tempDir.resolve("second-project")
        );

        ProjectRegistry registry = new ProjectRegistry(
                Map.of(
                        "first", firstProject,
                        "second", secondProject
                )
        );

        assertEquals(
                firstProject.toRealPath(),
                registry.getRoot("first")
        );
        assertEquals(
                secondProject.toRealPath(),
                registry.getRoot("second")
        );
    }

    @Test
    void shouldRejectNullProjects() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectRegistry(null)
        );

        assertEquals(
                "projects must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectEmptyProjects() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRegistry(Map.of())
        );

        assertEquals(
                "projects must not be empty",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProjectName() {
        Map<String, Path> projects = new HashMap<>();
        projects.put(null, tempDir);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectRegistry(projects)
        );

        assertEquals(
                "project name must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankProjectName() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRegistry(
                        Map.of(" ", tempDir)
                )
        );

        assertEquals(
                "project name must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProjectPath() {
        Map<String, Path> projects = new HashMap<>();
        projects.put("project", null);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ProjectRegistry(projects)
        );

        assertEquals(
                "project path must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectRelativeProjectPath() {
        Path relativePath = Path.of("relative-project");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRegistry(
                        Map.of("project", relativePath)
                )
        );

        assertEquals(
                "project path must be absolute: " + relativePath,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonExistingProjectPath() {
        Path missingPath = tempDir.resolve("missing-project");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRegistry(
                        Map.of("project", missingPath)
                )
        );

        assertEquals(
                "project path does not exist: " + missingPath,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectFileAsProjectRoot() throws IOException {
        Path file = Files.createFile(
                tempDir.resolve("project.txt")
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> new ProjectRegistry(
                        Map.of("project", file)
                )
        );

        assertEquals(
                "project path must be a directory: " + file,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProjectNameWhenGettingRoot() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", tempDir)
        );

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> registry.getRoot(null)
        );

        assertEquals(
                "project name must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankProjectNameWhenGettingRoot() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", tempDir)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getRoot(" ")
        );

        assertEquals(
                "project name must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnregisteredProject() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", tempDir)
        );

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> registry.getRoot("unknown")
        );

        assertEquals(
                "project is not registered: unknown",
                exception.getMessage()
        );
    }
}