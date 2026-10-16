package io.github.dsheirer.audio.broadcast.remote;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class RemoteApiBroadcasterTest
{
    @TempDir
    Path mTemporaryDirectory;

    @Test
    void sendsAuthenticatedHeartbeatAndMarksConnectionOnline() throws Exception
    {
        AtomicReference<String> body = new AtomicReference<>();
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> event = new AtomicReference<>();
        CountDownLatch received = new CountDownLatch(1);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/calls", exchange ->
        {
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            event.set(exchange.getRequestHeaders().getFirst("X-SDRTrunk-Event"));
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            received.countDown();
        });
        server.start();

        RemoteApiConfiguration configuration = new RemoteApiConfiguration();
        configuration.setName("Test Destination");
        configuration.setHost("http://127.0.0.1:" + server.getAddress().getPort() + "/calls");
        configuration.setApiKeyEnvironmentVariable("SDRTRUNK_TEST_MISSING_API_KEY");
        configuration.setApiKey("0123456789ABCDEF");
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(5);
        RemoteApiBroadcaster broadcaster = new RemoteApiBroadcaster(configuration);

        try
        {
            broadcaster.start();
            assertTrue(received.await(5, TimeUnit.SECONDS));
            for(int attempt = 0; attempt < 100 && broadcaster.getLastHeartbeatSuccess() == 0; attempt++)
            {
                Thread.sleep(10);
            }

            assertEquals("Bearer 0123456789ABCDEF", authorization.get());
            assertEquals("heartbeat", event.get());
            assertTrue(body.get().contains("\"event\":\"heartbeat\""));
            assertTrue(body.get().contains("\"status\":\"online\""));
            assertTrue(body.get().contains("\"destination\":\"Test Destination\""));
            assertTrue(broadcaster.getLastHeartbeatSuccess() > 0);
            assertTrue(broadcaster.getLastContactSuccess() > 0);
            assertEquals(BroadcastState.CONNECTED, broadcaster.getBroadcastState());
        }
        finally
        {
            broadcaster.stop();
            server.stop(0);
        }
    }

    @Test
    void replacesFailedConnectionAndRetriesHeartbeat() throws Exception
    {
        AtomicInteger requests = new AtomicInteger();
        CountDownLatch received = new CountDownLatch(2);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/calls", exchange ->
        {
            exchange.getRequestBody().readAllBytes();
            int request = requests.incrementAndGet();
            byte[] response = request == 1 ? "temporary upstream outage".getBytes(StandardCharsets.UTF_8) : new byte[0];
            exchange.sendResponseHeaders(request == 1 ? 503 : 204, response.length == 0 ? -1 : response.length);
            if(response.length > 0)
            {
                exchange.getResponseBody().write(response);
            }
            exchange.close();
            received.countDown();
        });
        server.start();

        RemoteApiConfiguration configuration = new RemoteApiConfiguration();
        configuration.setName("Recovery Test");
        configuration.setHost("http://127.0.0.1:" + server.getAddress().getPort() + "/calls");
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(5);
        configuration.setRequestTimeoutSeconds(1);
        RemoteApiBroadcaster broadcaster = new RemoteApiBroadcaster(configuration);

        try
        {
            broadcaster.start();
            assertTrue(received.await(6, TimeUnit.SECONDS));
            for(int attempt = 0; attempt < 100 && broadcaster.getLastHeartbeatSuccess() == 0; attempt++)
            {
                Thread.sleep(10);
            }

            assertTrue(requests.get() >= 2);
            assertTrue(broadcaster.getReconnectCount() >= 1);
            assertEquals(0, broadcaster.getConsecutiveConnectionFailures());
            assertTrue(broadcaster.getLastHeartbeatSuccess() > 0);
            assertEquals(BroadcastState.CONNECTED, broadcaster.getBroadcastState());
        }
        finally
        {
            broadcaster.stop();
            server.stop(0);
        }
    }

    @Test
    void copiesHeartbeatConfiguration()
    {
        RemoteApiConfiguration configuration = new RemoteApiConfiguration();
        assertFalse(configuration.isHeartbeatEnabled());
        configuration.setHeartbeatEnabled(true);
        configuration.setHeartbeatIntervalSeconds(45);
        RemoteApiConfiguration copy = (RemoteApiConfiguration)configuration.copyOf();

        assertTrue(copy.isHeartbeatEnabled());
        assertEquals(45, copy.getHeartbeatIntervalSeconds());
    }

    @Test
    void preservesQueuedCallsAcrossDestinationRestart()
    {
        RemoteApiConfiguration configuration = new RemoteApiConfiguration();
        configuration.setName("Queue Preservation Test");
        configuration.setHost("http://127.0.0.1/calls");
        AudioRecording recording = new AudioRecording(mTemporaryDirectory.resolve("call.mp3"),
            Collections.emptyList(), null, System.currentTimeMillis(), 1_000L);
        recording.addPendingReplay();

        RemoteApiBroadcaster first = new RemoteApiBroadcaster(configuration);
        first.receive(recording);
        assertEquals(1, first.getAudioQueueSize());
        first.stopPreservingQueue();

        RemoteApiBroadcaster replacement = new RemoteApiBroadcaster(configuration);
        assertEquals(1, replacement.getAudioQueueSize());
        assertTrue(recording.hasPendingReplays());

        replacement.stop();
        assertFalse(recording.hasPendingReplays());
    }

    @Test
    void continuouslyRetriesByDefaultAndCopiesSetting()
    {
        RemoteApiConfiguration configuration = new RemoteApiConfiguration();
        assertTrue(configuration.isRetryIndefinitely());
        configuration.setRetryIndefinitely(false);

        RemoteApiConfiguration copy = (RemoteApiConfiguration)configuration.copyOf();
        assertFalse(copy.isRetryIndefinitely());
    }
}
