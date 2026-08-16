package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PingToolTest {

    @Test
    void shouldCreatePingToolSpecification() {
        PingTool pingTool = new PingTool(new PingService());

        McpServerFeatures.SyncToolSpecification specification =
                pingTool.specification();

        assertEquals("ping", specification.tool().name());
        assertEquals("Ping", specification.tool().title());
        assertEquals(
                "Checks that Project Context is available",
                specification.tool().description()
        );
    }

    @Test
    void shouldExposeToolAnnotations() {
        PingTool pingTool = new PingTool(new PingService());

        McpSchema.ToolAnnotations annotations =
                pingTool.specification().tool().annotations();

        assertTrue(annotations.readOnlyHint());
        assertFalse(annotations.destructiveHint());
        assertFalse(annotations.openWorldHint());
        assertTrue(annotations.idempotentHint());
    }

    @Test
    void shouldExposeInputSchema() {
        PingTool pingTool = new PingTool(new PingService());

        Map<String, Object> inputSchema =
                pingTool.specification().tool().inputSchema();

        assertEquals("object", inputSchema.get("type"));
        assertEquals(Map.of(), inputSchema.get("properties"));
        assertEquals(false, inputSchema.get("additionalProperties"));
    }

    @Test
    void shouldExposeOutputSchema() {
        PingTool pingTool = new PingTool(new PingService());

        Map<String, Object> outputSchema =
                pingTool.specification().tool().outputSchema();

        assertEquals("object", outputSchema.get("type"));
        assertEquals(
                Map.of(
                        "message",
                        Map.of("type", "string")
                ),
                outputSchema.get("properties")
        );
        assertEquals(List.of("message"), outputSchema.get("required"));
        assertEquals(false, outputSchema.get("additionalProperties"));
    }

    @Test
    void shouldExposePublicVisibility() {
        PingTool pingTool = new PingTool(new PingService());

        Map<String, Object> meta =
                pingTool.specification().tool().meta();

        assertEquals("public", meta.get("openai/visibility"));
    }

    @Test
    void shouldRejectNullPingService() {
        NullPointerException exception = assertThrows(
                NullPointerException.class,
                () -> new PingTool(null)
        );

        assertEquals("pingService must not be null", exception.getMessage());
    }

    @Test
    void shouldReturnPingResponse() {
        PingTool pingTool = new PingTool(new PingService());

        McpServerFeatures.SyncToolSpecification specification =
                pingTool.specification();

        McpSchema.CallToolRequest request =
                McpSchema.CallToolRequest.builder("ping")
                        .arguments(Map.of())
                        .build();

        McpSchema.CallToolResult result =
                specification.callHandler().apply(null, request);

        McpSchema.TextContent content =
                (McpSchema.TextContent) result.content().getFirst();

        assertEquals("hello from Project Context", content.text());
        assertEquals(
                Map.of("message", "hello from Project Context"),
                result.structuredContent()
        );
        assertFalse(result.isError());
    }
}