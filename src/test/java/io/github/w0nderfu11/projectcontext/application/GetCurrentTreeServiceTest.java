package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GetCurrentTreeServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReturnCurrentDirectoryTree() throws IOException {
        Path directory = Files.createDirectory(
                tempDir.resolve("src")
        );

        Path nestedDirectory = Files.createDirectory(
                directory.resolve("main")
        );

        Path javaFile = Files.createFile(
                directory.resolve("Example.java")
        );

        Path textFile = Files.createFile(
                directory.resolve("README.txt")
        );

        GetCurrentTreeService service = service();

        Map<String, TreeEntryType> entries = service.get(
                "project",
                directory
        );

        assertEquals(3, entries.size());
        assertEquals(
                TreeEntryType.DIRECTORY,
                entries.get(nestedDirectory.toString())
        );
        assertEquals(
                TreeEntryType.FILE,
                entries.get(javaFile.toString())
        );
        assertEquals(
                TreeEntryType.FILE,
                entries.get(textFile.toString())
        );
    }

    @Test
    void shouldReturnOnlyImmediateChildren() throws IOException {
        Path directory = Files.createDirectory(
                tempDir.resolve("src")
        );

        Path nestedDirectory = Files.createDirectory(
                directory.resolve("main")
        );

        Path nestedFile = Files.createFile(
                nestedDirectory.resolve("Example.java")
        );

        GetCurrentTreeService service = service();

        Map<String, TreeEntryType> entries = service.get(
                "project",
                directory
        );

        assertEquals(1, entries.size());
        assertEquals(
                TreeEntryType.DIRECTORY,
                entries.get(nestedDirectory.toString())
        );
        assertFalse(
                entries.containsKey(nestedFile.toString())
        );
    }

    @Test
    void shouldReturnEmptyTreeForEmptyDirectory() throws IOException {
        Path directory = Files.createDirectory(
                tempDir.resolve("empty")
        );

        GetCurrentTreeService service = service();

        Map<String, TreeEntryType> entries = service.get(
                "project",
                directory
        );

        assertTrue(entries.isEmpty());
    }

    @Test
    void shouldRejectNullRegistry() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new GetCurrentTreeService(null)
        );

        assertEquals(
                "projectRegistry must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProjectName() throws IOException {
        GetCurrentTreeService service = service();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.get(null, tempDir)
        );

        assertEquals(
                "projectName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullDirectoryPath() throws IOException {
        GetCurrentTreeService service = service();

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.get("project", null)
        );

        assertEquals(
                "directoryPath must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnregisteredProject() throws IOException {
        GetCurrentTreeService service = service();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.get("unknown", tempDir)
        );

        assertEquals(
                "project is not registered: unknown",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectDirectoryOutsideProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path outside = Files.createDirectory(
                tempDir.resolve("outside")
        );

        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", project)
        );

        GetCurrentTreeService service =
                new GetCurrentTreeService(registry);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.get("project", outside)
        );

        assertEquals(
                "directory path is outside registered project root: "
                        + outside,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonExistingDirectory() throws IOException {
        Path missing = tempDir.resolve("missing");

        GetCurrentTreeService service = service();

        assertThrows(
                IOException.class,
                () -> service.get("project", missing)
        );
    }

    @Test
    void shouldRejectFileInsteadOfDirectory() throws IOException {
        Path file = Files.createFile(
                tempDir.resolve("Example.java")
        );

        GetCurrentTreeService service = service();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.get("project", file)
        );

        assertEquals(
                "directory path must point to a directory: "
                        + file.toRealPath(),
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectSymlinkEscapeOutsideProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path outside = Files.createDirectory(
                tempDir.resolve("outside")
        );

        Path link = project.resolve("outside-link");

        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException e) {
            Assumptions.assumeTrue(
                    false,
                    "Symbolic links are not supported in this environment"
            );
        }

        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", project)
        );

        GetCurrentTreeService service =
                new GetCurrentTreeService(registry);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.get("project", link)
        );

        assertEquals(
                "real directory path is outside registered project root: "
                        + outside.toRealPath(),
                exception.getMessage()
        );
    }

    private GetCurrentTreeService service() throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of(
                        "project",
                        tempDir
                )
        );

        return new GetCurrentTreeService(registry);
    }
}