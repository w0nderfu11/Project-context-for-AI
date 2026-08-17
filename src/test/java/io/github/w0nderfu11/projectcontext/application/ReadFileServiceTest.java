package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReadFileServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReadFileAndPreserveContent() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.writeString(
                project.resolve("Example.java"),
                """
                public class Example {

                    public void execute() {
                        System.out.println("hello");
                    }
                }
                """
        );

        ReadFileService service = service(project);

        String content = service.read("project", file);

        assertEquals(
                Files.readString(file),
                content
        );
    }

    @Test
    void shouldReadFileFromNestedDirectory() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path sourceDirectory = Files.createDirectories(
                project.resolve("src/main/java")
        );

        Path file = Files.writeString(
                sourceDirectory.resolve("Example.java"),
                "public class Example {}"
        );

        ReadFileService service = service(project);

        assertEquals(
                "public class Example {}",
                service.read("project", file)
        );
    }

    @Test
    void shouldRejectNullRegistry() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new ReadFileService(null)
        );

        assertEquals(
                "projectRegistry must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullProjectName() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("Example.java")
        );

        ReadFileService service = service(project);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.read(null, file)
        );

        assertEquals(
                "projectName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullFilePath() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        ReadFileService service = service(project);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.read("project", null)
        );

        assertEquals(
                "filePath must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnregisteredProject() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("Example.java")
        );

        ReadFileService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.read("unknown", file)
        );

        assertEquals(
                "project is not registered: unknown",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectFileOutsideProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path outsideFile = Files.createFile(
                tempDir.resolve("outside.txt")
        );

        ReadFileService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.read("project", outsideFile)
        );

        assertEquals(
                "file path is outside registered project root: " + outsideFile,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonExistingFile() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path missingFile = project.resolve("missing.txt");

        ReadFileService service = service(project);

        assertThrows(
                NoSuchFileException.class,
                () -> service.read("project", missingFile)
        );
    }

    @Test
    void shouldRejectDirectoryAsFile() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path directory = Files.createDirectory(
                project.resolve("directory")
        );

        ReadFileService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.read("project", directory)
        );

        assertEquals(
                "file path must point to a regular file: " + directory.toRealPath(),
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectSymlinkOutsideProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path outsideDirectory = Files.createDirectory(
                tempDir.resolve("outside")
        );

        Path outsideFile = Files.writeString(
                outsideDirectory.resolve("secret.txt"),
                "secret"
        );

        Path link = project.resolve("external-link");

        try {
            Files.createSymbolicLink(link, outsideDirectory);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            assumeTrue(false, "Symbolic links are not available");
        }

        Path linkedFile = link.resolve("secret.txt");

        ReadFileService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.read("project", linkedFile)
        );

        assertEquals(
                "real file path is outside registered project root: "
                        + outsideFile.toRealPath(),
                exception.getMessage()
        );
    }

    private ReadFileService service(Path project) throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", project)
        );

        return new ReadFileService(registry);
    }
}