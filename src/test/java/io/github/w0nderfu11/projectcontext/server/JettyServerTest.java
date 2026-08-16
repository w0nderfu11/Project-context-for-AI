package io.github.w0nderfu11.projectcontext.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JettyServerTest {

    private static final Handler HANDLER = new Handler.Abstract() {
        @Override
        public boolean handle(Request request, Response response, Callback callback) {
            callback.succeeded();
            return true;
        }
    };

    @Test
    void shouldStartAndStopServer() throws Exception {
        JettyServer server = new JettyServer("127.0.0.1", 0, HANDLER);

        assertFalse(server.isRunning());

        try {
            server.start();

            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }

        assertFalse(server.isRunning());
    }

    @Test
    void shouldAssignLocalPortWhenConfiguredWithZero() throws Exception {
        JettyServer server = new JettyServer("127.0.0.1", 0, HANDLER);

        try {
            server.start();

            assertTrue(server.localPort() > 0);
        } finally {
            server.stop();
        }
    }

    @Test
    void shouldRejectNullHost() {
        assertThrows(
                NullPointerException.class,
                () -> new JettyServer(null, 8080, HANDLER)
        );
    }

    @Test
    void shouldRejectBlankHost() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer(" ", 8080, HANDLER)
        );
    }

    @Test
    void shouldRejectNegativePort() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer("127.0.0.1", -1, HANDLER)
        );
    }

    @Test
    void shouldRejectPortAboveMaximum() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new JettyServer("127.0.0.1", 65536, HANDLER)
        );
    }

    @Test
    void shouldRejectNullHandler() {
        assertThrows(
                NullPointerException.class,
                () -> new JettyServer("127.0.0.1", 8080, null)
        );
    }
}