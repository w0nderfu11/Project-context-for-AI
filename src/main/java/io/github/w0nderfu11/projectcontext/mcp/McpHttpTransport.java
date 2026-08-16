package io.github.w0nderfu11.projectcontext.mcp;

import io.modelcontextprotocol.server.transport.HttpServletStreamableServerTransportProvider;
import org.eclipse.jetty.ee10.servlet.ServletContextHandler;
import org.eclipse.jetty.server.Handler;

public final class McpHttpTransport {

    private final HttpServletStreamableServerTransportProvider transport;
    private final ServletContextHandler handler;

    public McpHttpTransport() {
        this.transport = HttpServletStreamableServerTransportProvider.builder()
                .mcpEndpoint(McpEndpoint.MCP)
                .build();

        this.handler = new ServletContextHandler();
        this.handler.setContextPath("/");
        this.handler.addServlet(transport, McpEndpoint.MCP);
    }

    public HttpServletStreamableServerTransportProvider transport() {
        return transport;
    }

    public Handler handler() {
        return handler;
    }
}