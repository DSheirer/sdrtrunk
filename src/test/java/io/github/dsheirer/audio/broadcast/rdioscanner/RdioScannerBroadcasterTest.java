package io.github.dsheirer.audio.broadcast.rdioscanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.identifier.IdentifierCollection;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RdioScannerBroadcasterTest
{
    @TempDir
    Path mTemporaryDirectory;

    @Test
    void sendsConfiguredHeartbeatWithAuthenticationAndSystemIdentity() throws Exception
    {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        AtomicReference<String> systemId = new AtomicReference<>();
        AtomicReference<String> event = new AtomicReference<>();
        CountDownLatch heartbeatReceived = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/call-upload", exchange ->
        {
            exchange.getRequestBody().readAllBytes();
            byte[] response = "Incomplete call data: no talkgroup".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.createContext("/scanner-heartbeat", exchange ->
        {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            apiKey.set(exchange.getRequestHeaders().getFirst("X-API-Key"));
            systemId.set(exchange.getRequestHeaders().getFirst("X-RdioScanner-System-Id"));
            event.set(exchange.getRequestHeaders().getFirst("X-SDRTrunk-Event"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            heartbeatReceived.countDown();
        });
        server.start();

        RdioScannerConfiguration configuration = new RdioScannerConfiguration();
        configuration.setName("County Rdio");
        configuration.setHost("http://127.0.0.1:" + server.getAddress().getPort() + "/api/call-upload");
        configuration.setApiKey("0123456789ABCDEF");
        configuration.setSystemID(42);
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(5);
        configuration.setHeartbeatUrl("http://127.0.0.1:" + server.getAddress().getPort() +
            "/scanner-heartbeat");
        RdioScannerBroadcaster broadcaster = new RdioScannerBroadcaster(configuration, null, null, null);

        try
        {
            broadcaster.start();
            assertTrue(heartbeatReceived.await(5, TimeUnit.SECONDS));
            for(int attempt = 0; attempt < 100 && broadcaster.getLastHeartbeatSuccess() == 0; attempt++)
            {
                Thread.sleep(10);
            }

            assertEquals("0123456789ABCDEF", apiKey.get());
            assertEquals("42", systemId.get());
            assertEquals("heartbeat", event.get());
            assertTrue(body.get().contains("\"event\":\"heartbeat\""));
            assertTrue(body.get().contains("\"destination\":\"County Rdio\""));
            assertTrue(body.get().contains("\"systemId\":42"));
            assertTrue(broadcaster.getLastHeartbeatSuccess() > 0);
        }
        finally
        {
            broadcaster.stop();
            server.stop(0);
        }
    }

    @Test
    void derivesHeartbeatUrlAndCopiesSettings()
    {
        RdioScannerConfiguration configuration = new RdioScannerConfiguration();
        configuration.setName("Rdio");
        configuration.setHost("https://scanner.example/api/call-upload");
        configuration.setApiKey("secret");
        configuration.setSystemID(7);
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(45);

        assertEquals("https://scanner.example/api/heartbeat", configuration.resolveHeartbeatUrl());
        RdioScannerConfiguration copy = (RdioScannerConfiguration)configuration.copyOf();
        assertEquals("https://scanner.example/api/call-upload", copy.getHost());
        assertEquals("secret", copy.getApiKey());
        assertEquals(7, copy.getSystemID());
        assertTrue(copy.isHeartbeatEnabled());
        assertEquals(45, copy.getHeartbeatIntervalSeconds());

        configuration.setHeartbeatUrl("https://status.example/scanner/online");
        assertEquals("https://status.example/scanner/online", configuration.resolveHeartbeatUrl());
        configuration.setHeartbeatEnabled(false);
        assertFalse(configuration.isHeartbeatEnabled());
    }

    @Test
    void createsDisabledCloneWithRdioSettingsIntact()
    {
        RdioScannerConfiguration configuration = new RdioScannerConfiguration();
        configuration.setName("Primary Rdio");
        configuration.setHost("https://scanner.example/api/call-upload");
        configuration.setApiKey("secret");
        configuration.setSystemID(19);
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(30);
        configuration.setHeartbeatUrl("https://scanner.example/status");
        configuration.setEnabled(true);

        RdioScannerConfiguration clone = (RdioScannerConfiguration)configuration.copyOfDisabled();

        assertEquals(configuration.getName(), clone.getName());
        assertEquals(configuration.getHost(), clone.getHost());
        assertEquals(configuration.getApiKey(), clone.getApiKey());
        assertEquals(configuration.getSystemID(), clone.getSystemID());
        assertEquals(configuration.getHeartbeatUrl(), clone.getHeartbeatUrl());
        assertEquals(configuration.getHeartbeatIntervalSeconds(), clone.getHeartbeatIntervalSeconds());
        assertTrue(clone.isHeartbeatEnabled());
        assertFalse(clone.isEnabled());
    }

    @Test
    void restoresQueuedCallAfterRestartAndRetriesUntilHostReturns() throws Exception
    {
        int port;
        try(ServerSocket socket = new ServerSocket(0))
        {
            port = socket.getLocalPort();
        }

        RdioScannerConfiguration configuration = new RdioScannerConfiguration();
        configuration.setName("Restart Safe Rdio");
        configuration.setHost("http://127.0.0.1:" + port + "/api/call-upload");
        configuration.setApiKey("secret");
        configuration.setSystemID(23);
        Path source = mTemporaryDirectory.resolve("temporary_streaming_file_1_20261012_120000.mp3");
        Files.write(source, "test-audio".getBytes(StandardCharsets.UTF_8));
        AudioRecording recording = new AudioRecording(source, List.of(), new IdentifierCollection(),
            System.currentTimeMillis(), 1000);
        recording.addPendingReplay();

        RdioScannerBroadcaster first = new RdioScannerBroadcaster(configuration, null, null, null,
            mTemporaryDirectory);
        first.receive(recording);
        assertEquals(1, first.getAudioQueueSize());
        first.stop();

        RdioScannerBroadcaster restarted = new RdioScannerBroadcaster(configuration, null, null, null,
            mTemporaryDirectory);
        assertEquals(1, restarted.getAudioQueueSize());
        restarted.start();

        //Allow the initial connection to fail before the server becomes available.
        for(int attempt = 0; attempt < 100 && restarted.getLastConnectionError().isBlank(); attempt++)
        {
            Thread.sleep(20);
        }
        assertFalse(restarted.getLastConnectionError().isBlank());

        CountDownLatch uploadReceived = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        server.createContext("/api/call-upload", exchange ->
        {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.ISO_8859_1);
            boolean test = body.contains("name=\"test\"");
            byte[] response = (test ? "Incomplete call data: no talkgroup" : "Call imported successfully.")
                .getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
            if(!test)
            {
                uploadReceived.countDown();
            }
        });
        server.start();

        try
        {
            assertTrue(uploadReceived.await(10, TimeUnit.SECONDS));
            for(int attempt = 0; attempt < 100 && restarted.getAudioQueueSize() != 0; attempt++)
            {
                Thread.sleep(20);
            }
            assertEquals(0, restarted.getAudioQueueSize());
            assertTrue(restarted.getLastConnectionSuccess() > 0);
            assertEquals(0, RdioScannerBroadcaster.getPersistedQueueSize(configuration, mTemporaryDirectory));
        }
        finally
        {
            restarted.stop();
            server.stop(0);
        }
    }
}
