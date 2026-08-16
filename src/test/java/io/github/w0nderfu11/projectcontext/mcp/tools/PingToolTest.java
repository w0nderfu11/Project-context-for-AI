package io.github.w0nderfu11.projectcontext.mcp.tools;

import io.github.w0nderfu11.projectcontext.application.PingService;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PingToolTest {

    @Test
    void shouldCreatePingToolSpecification() {
        PingTool pingTool = new PingTool(new PingService());

        McpServerFeatures.SyncToolSpecification specification =
                pingTool.specification();

        assertEquals("ping", specification.tool().name());
        assertEquals(
                "Checks that Project Context is available",
                specification.tool().description()
        );
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
        assertFalse(result.isError());
    }
}