package io.github.w0nderfu11.projectcontext.server;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.eclipse.jetty.util.Callback;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JettyServerTest {

    @Test
    void shouldStartAndStopServer() throws Exception {
        Handler handler = new Handler.Abstract() {
            @Override
            public boolean handle(Request request, Response response, Callback callback) {
                callback.succeeded();
                return true;
            }
        };

        JettyServer server = new JettyServer("127.0.0.1", 0, handler);

        assertFalse(server.isRunning());

        try {
            server.start();

            assertTrue(server.isRunning());
        } finally {
            server.stop();
        }

        assertFalse(server.isRunning());
    }
}