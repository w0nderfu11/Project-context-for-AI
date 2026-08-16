package io.github.w0nderfu11.projectcontext.application;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PingServiceTest {

    @Test
    void shouldReturnPingResponse() {
        PingService service = new PingService();

        String response = service.ping();

        assertEquals("hello from Project Context", response);
    }
}