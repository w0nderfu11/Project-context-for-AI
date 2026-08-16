package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.server.JettyServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHttpTransportTest {

    @Test
    void shouldCreateTransport() {
        McpHttpTransport mcpTransport = new McpHttpTransport();

        assertNotNull(mcpTransport.transport());
        assertNotNull(mcpTransport.handler());
    }

    @Test
    void shouldExposeHandlerThatCanRunWithJetty() throws Exception {
        McpHttpTransport mcpTransport = new McpHttpTransport();

        JettyServer server = new JettyServer(
                "127.0.0.1",
                0,
                mcpTransport.handler()
        );

        try {
            server.start();

            assertTrue(server.isRunning());
            assertTrue(server.localPort() > 0);
        } finally {
            server.stop();
        }
    }
}