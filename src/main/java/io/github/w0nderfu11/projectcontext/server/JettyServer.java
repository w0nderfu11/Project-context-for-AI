package io.github.w0nderfu11.projectcontext.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.GracefulHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public final class JettyServer {

    private static final Logger log = LoggerFactory.getLogger(JettyServer.class);

    private static final long STOP_TIMEOUT_MS = 5_000;

    private final String host;
    private final int port;
    private final Server server;
    private final ServerConnector connector;

    public JettyServer(String host, int port, Handler handler) {
        this.host = validateHost(host);
        this.port = validatePort(port);

        Objects.requireNonNull(handler, "handler must not be null");

        this.server = new Server();
        this.connector = new ServerConnector(server);

        connector.setHost(this.host);
        connector.setPort(this.port);

        server.addConnector(connector);
        server.setHandler(new GracefulHandler(handler));

        server.setStopTimeout(STOP_TIMEOUT_MS);
        server.setStopAtShutdown(true);
    }

    public void start() throws Exception {
        log.debug("Starting HTTP server on {}:{}", host, port);

        server.start();

        log.info("HTTP server started on {}:{}", host, localPort());
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
        return connector.getLocalPort();
    }

    public void join() throws InterruptedException {
        server.join();
    }

    private static String validateHost(String host) {
        Objects.requireNonNull(host, "host must not be null");

        if (host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }

        return host;
    }

    private static int validatePort(int port) {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("port must be between 0 and 65535");
        }

        return port;
    }
}