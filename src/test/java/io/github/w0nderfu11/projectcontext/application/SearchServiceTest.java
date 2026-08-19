package io.github.w0nderfu11.projectcontext.application;

import io.github.w0nderfu11.projectcontext.registry.ProjectRegistry;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldFindFileByNameAndExtension() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("ProjectContextApplication.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "ProjectContextApplication",
                "java",
                null
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(file.toRealPath().toString())
        );
    }

    @Test
    void shouldFindFileByPartialName() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("ProjectContextApplication.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "ntex",
                "java",
                null
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(file.toRealPath().toString())
        );
    }

    @Test
    void shouldIgnoreCaseInFileName() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("ProjectContextApplication.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "context",
                "java",
                null
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(file.toRealPath().toString())
        );
    }

    @Test
    void shouldMatchExtensionExactlyIgnoringCase() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path javaFile = Files.createFile(
                project.resolve("Example.JAVA")
        );

        Files.createFile(
                project.resolve("Example.javascript")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "example",
                "java",
                null
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(javaFile.toRealPath().toString())
        );
    }

    @Test
    void shouldSearchRecursivelyFromProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path sourceDirectory = Files.createDirectories(
                project.resolve("src/main/java/application")
        );

        Path file = Files.createFile(
                sourceDirectory.resolve("SearchService.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "search",
                "java",
                null
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(file.toRealPath().toString())
        );
    }

    @Test
    void shouldSearchOnlyInsideSpecifiedDirectory() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path mainDirectory = Files.createDirectories(
                project.resolve("src/main")
        );

        Path testDirectory = Files.createDirectories(
                project.resolve("src/test")
        );

        Path mainFile = Files.createFile(
                mainDirectory.resolve("Example.java")
        );

        Path testFile = Files.createFile(
                testDirectory.resolve("Example.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "example",
                "java",
                mainDirectory
        );

        assertEquals(1, result.size());
        assertTrue(
                result.contains(mainFile.toRealPath().toString())
        );
        assertFalse(
                result.contains(testFile.toRealPath().toString())
        );
    }

    @Test
    void shouldReturnMultipleMatches() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path firstFile = Files.createFile(
                project.resolve("ProjectContextApplication.java")
        );

        Path secondFile = Files.createFile(
                project.resolve("ProjectContextMcpServer.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "context",
                "java",
                null
        );

        assertEquals(2, result.size());
        assertTrue(
                result.contains(firstFile.toRealPath().toString())
        );
        assertTrue(
                result.contains(secondFile.toRealPath().toString())
        );
    }

    @Test
    void shouldReturnEmptyListWhenNoMatchesFound() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Files.createFile(
                project.resolve("Example.java")
        );

        SearchService service = service(project);

        List<String> result = service.search(
                "project",
                "missing",
                "java",
                null
        );

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldRejectNullRegistry() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new SearchService(null)
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

        SearchService service = service(project);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.search(
                        null,
                        "example",
                        "java",
                        null
                )
        );

        assertEquals(
                "projectName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullFileName() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        SearchService service = service(project);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.search(
                        "project",
                        null,
                        "java",
                        null
                )
        );

        assertEquals(
                "fileName must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankFileName() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "project",
                        " ",
                        "java",
                        null
                )
        );

        assertEquals(
                "fileName must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNullExtension() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        SearchService service = service(project);

        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> service.search(
                        "project",
                        "example",
                        null,
                        null
                )
        );

        assertEquals(
                "extension must not be null",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectBlankExtension() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "project",
                        "example",
                        " ",
                        null
                )
        );

        assertEquals(
                "extension must not be blank",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectUnregisteredProject() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "unknown",
                        "example",
                        "java",
                        null
                )
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

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "project",
                        "example",
                        "java",
                        outside
                )
        );

        assertEquals(
                "directory path is outside registered project root: "
                        + outside,
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectNonExistingDirectory() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path missing = project.resolve("missing");

        SearchService service = service(project);

        assertThrows(
                NoSuchFileException.class,
                () -> service.search(
                        "project",
                        "example",
                        "java",
                        missing
                )
        );
    }

    @Test
    void shouldRejectFileInsteadOfDirectory() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path file = Files.createFile(
                project.resolve("Example.java")
        );

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "project",
                        "example",
                        "java",
                        file
                )
        );

        assertEquals(
                "directory path must point to a directory: "
                        + file.toRealPath(),
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectSymlinkDirectoryOutsideProjectRoot() throws IOException {
        Path project = Files.createDirectory(
                tempDir.resolve("project")
        );

        Path outside = Files.createDirectory(
                tempDir.resolve("outside")
        );

        Path link = project.resolve("outside-link");

        try {
            Files.createSymbolicLink(link, outside);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            Assumptions.assumeTrue(
                    false,
                    "Symbolic links are not available"
            );
        }

        SearchService service = service(project);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.search(
                        "project",
                        "example",
                        "java",
                        link
                )
        );

        assertEquals(
                "real directory path is outside registered project root: "
                        + outside.toRealPath(),
                exception.getMessage()
        );
    }

    private SearchService service(Path project) throws IOException {
        ProjectRegistry registry = new ProjectRegistry(
                Map.of("project", project)
        );

        return new SearchService(registry);
    }
}