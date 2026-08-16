package io.github.w0nderfu11.projectcontext.mcp;

import io.github.w0nderfu11.projectcontext.server.JettyServer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McpHttpTransportTest {

    @Test
    void shouldCreateTransportAndStartWithJetty() throws Exception {
        McpHttpTransport mcpTransport = new McpHttpTransport();
        JettyServer server = new JettyServer(
                "127.0.0.1",
                0,
                mcpTransport.handler()
        );

        assertNotNull(mcpTransport.transport());

        try {
            server.start();

            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }
    }
}