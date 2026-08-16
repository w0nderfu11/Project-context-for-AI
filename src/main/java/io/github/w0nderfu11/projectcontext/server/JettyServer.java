package io.github.w0nderfu11.projectcontext.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class JettyServer {

    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

    private final String host;
    private final int port;
    private final Server server;

    public JettyServer(String host, int port, Handler handler) {
        this.host = host;
        this.port = port;

        this.server = new Server();

        ServerConnector connector = new ServerConnector(server);
        connector.setHost(host);
        connector.setPort(port);

        server.addConnector(connector);
        server.setHandler(handler);
    }

    public void start() throws Exception {
        log.debug("Starting HTTP server on {}:{}", host, port);

        server.start();

        log.info("HTTP server started on {}:{}", host, port);
    }

    public void stop() throws Exception {
        log.debug("Stopping HTTP server");

        server.stop();

        log.info("HTTP server stopped");
    }

    public boolean isRunning() {
        return server.isRunning();
    }

    public int localPort() {
        return ((ServerConnector) server.getConnectors()[0]).getLocalPort();
    }
}
