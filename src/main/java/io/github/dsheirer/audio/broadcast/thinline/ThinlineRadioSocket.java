/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.audio.broadcast.thinline;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One websocket to Thinline Radio. SDRTrunk sends health and upload check-ins. The server sends commands.
 */
public class ThinlineRadioSocket implements WebSocket.Listener
{
    private static final Logger mLog = LoggerFactory.getLogger(ThinlineRadioSocket.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final ConcurrentHashMap<String, ThinlineRadioSocket> SHARED = new ConcurrentHashMap<>();
    private static final HttpClient SHARED_CLIENT = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    private final HttpClient mHttpClient;
    private final List<Consumer<JsonNode>> mCommandListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> mReadyListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> mClosedListeners = new CopyOnWriteArrayList<>();
    private final Object mSendLock = new Object();
    private final StringBuilder mText = new StringBuilder();
    private final AtomicBoolean mReady = new AtomicBoolean();

    private volatile WebSocket mWebSocket;
    private volatile String mUrl = "";
    private volatile String mApiKey = "";
    private volatile boolean mConnecting;
    private CompletableFuture<Void> mSendChain = CompletableFuture.completedFuture(null);

    ThinlineRadioSocket(HttpClient httpClient)
    {
        mHttpClient = httpClient;
    }

    /**
     * One socket for a site key. A second caller with the same URL and key joins this socket.
     */
    /**
     * Opens the site websocket, sends the site key, and waits for ThinLine to accept it.
     * @return null when ThinLine welcomes the key, otherwise a message safe to show the user
     */
    public static String testConnection(String host, String apiKey)
    {
        if(apiKey == null || apiKey.isBlank())
        {
            return "Enter the site key.";
        }

        String url = socketUrl(host);
        if(url == null)
        {
            return "Enter a URL that starts with http:// or https://.";
        }

        String key = apiKey.trim();
        ThinlineRadioSocket live = SHARED.get(url + "\n" + key);
        if(live != null && live.isReady())
        {
            return null;
        }

        CompletableFuture<String> result = new CompletableFuture<>();
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
        client.newWebSocketBuilder().connectTimeout(Duration.ofSeconds(8))
            .buildAsync(URI.create(url), new TestListener(key, result))
            .whenComplete((webSocket, error) -> {
                if(error != null)
                {
                    result.complete(reachableMessage(error));
                }
            });

        try
        {
            return result.get(8, TimeUnit.SECONDS);
        }
        catch(TimeoutException e)
        {
            return "ThinLine did not answer. Check the URL and that this computer can reach it.";
        }
        catch(Exception e)
        {
            return reachableMessage(e);
        }
    }

    static String socketUrl(String host)
    {
        if(host == null)
        {
            return null;
        }

        String trimmed = host.trim();
        String uploadPath = "/api/call-upload";
        if(trimmed.endsWith(uploadPath))
        {
            trimmed = trimmed.substring(0, trimmed.length() - uploadPath.length());
        }
        while(trimmed.endsWith("/"))
        {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }

        if(trimmed.startsWith("https://"))
        {
            return "wss://" + trimmed.substring("https://".length()) + "/api/sdrtrunk";
        }
        if(trimmed.startsWith("http://"))
        {
            return "ws://" + trimmed.substring("http://".length()) + "/api/sdrtrunk";
        }
        return null;
    }

    private static String reachableMessage(Throwable error)
    {
        String message = error.getMessage();
        if(message == null || message.isBlank())
        {
            return "Could not reach ThinLine.";
        }
        return "Could not reach ThinLine. " + message;
    }

    private static final class TestListener implements WebSocket.Listener
    {
        private final String mApiKey;
        private final CompletableFuture<String> mResult;
        private final StringBuilder mText = new StringBuilder();

        private TestListener(String apiKey, CompletableFuture<String> result)
        {
            mApiKey = apiKey;
            mResult = result;
        }

        @Override
        public void onOpen(WebSocket webSocket)
        {
            webSocket.request(1);
            ObjectNode hello = MAPPER.createObjectNode();
            hello.put("type", "hello");
            hello.put("key", mApiKey);
            try
            {
                webSocket.sendText(MAPPER.writeValueAsString(hello), true);
            }
            catch(Exception e)
            {
                mResult.complete("Could not send the site key.");
                webSocket.abort();
            }
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
        {
            mText.append(data);
            if(last)
            {
                String message = mText.toString();
                mText.setLength(0);
                try
                {
                    JsonNode node = MAPPER.readTree(message);
                    String type = node.path("type").asText("");
                    if("welcome".equals(type))
                    {
                        mResult.complete(null);
                    }
                    else if("error".equals(type))
                    {
                        String error = node.path("error").asText("");
                        if(error.contains("site control"))
                        {
                            mResult.complete("That key is not a site key. Use the site control key, not an upload key.");
                        }
                        else
                        {
                            mResult.complete(error.isBlank() ? "ThinLine refused the connection." : error);
                        }
                    }
                }
                catch(Exception e)
                {
                    mResult.complete("ThinLine sent a response SDRTrunk could not read.");
                }
                webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "test");
            }
            webSocket.request(1);
            return null;
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
        {
            mResult.complete("ThinLine closed the connection.");
            return null;
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error)
        {
            mResult.complete(reachableMessage(error));
        }
    }

    static ThinlineRadioSocket share(String url, String apiKey)
    {
        String id = url + "\n" + apiKey;
        return SHARED.compute(id, (key, existing) -> {
            if(existing != null && (existing.mWebSocket != null || existing.mConnecting || existing.mReady.get()))
            {
                return existing;
            }
            if(existing != null)
            {
                existing.closeSocket();
            }
            ThinlineRadioSocket created = new ThinlineRadioSocket(SHARED_CLIENT);
            created.ensure(url, apiKey);
            return created;
        });
    }

    void addCommandListener(Consumer<JsonNode> listener)
    {
        if(listener != null && !mCommandListeners.contains(listener))
        {
            mCommandListeners.add(listener);
        }
    }

    void removeCommandListener(Consumer<JsonNode> listener)
    {
        mCommandListeners.remove(listener);
        closeIfIdle();
    }

    void addClosedListener(Runnable listener)
    {
        if(listener != null && !mClosedListeners.contains(listener))
        {
            mClosedListeners.add(listener);
        }
    }

    void removeClosedListener(Runnable listener)
    {
        mClosedListeners.remove(listener);
        closeIfIdle();
    }

    private void closeIfIdle()
    {
        if(mCommandListeners.isEmpty() && mReadyListeners.isEmpty() && mClosedListeners.isEmpty())
        {
            SHARED.values().remove(this);
            closeSocket();
        }
    }

    void addReadyListener(Runnable listener)
    {
        if(listener == null || mReadyListeners.contains(listener))
        {
            if(listener != null && mReady.get())
            {
                listener.run();
            }
            return;
        }
        mReadyListeners.add(listener);
        if(mReady.get())
        {
            listener.run();
        }
    }

    void removeReadyListener(Runnable listener)
    {
        mReadyListeners.remove(listener);
        closeIfIdle();
    }

    void ensure(String url, String apiKey)
    {
        if(url == null || url.isBlank() || apiKey == null || apiKey.isBlank())
        {
            closeSocket();
            return;
        }

        if(url.equals(mUrl) && apiKey.equals(mApiKey) && (mWebSocket != null || mConnecting))
        {
            return;
        }

        closeSocket();
        mUrl = url;
        mApiKey = apiKey;
        mConnecting = true;
        mHttpClient.newWebSocketBuilder().buildAsync(URI.create(url), this).whenComplete((webSocket, error) -> {
            mConnecting = false;
            if(error != null)
            {
                mLog.warn("Thinline Radio websocket {} failed: {}", url, error.toString());
                disconnected(null);
            }
        });
    }

    boolean isReady()
    {
        return mReady.get() && mWebSocket != null;
    }

    boolean send(ObjectNode body)
    {
        WebSocket webSocket = mWebSocket;
        if(!isReady() || webSocket == null)
        {
            return false;
        }

        String text;
        try
        {
            text = MAPPER.writeValueAsString(body);
        }
        catch(Exception e)
        {
            return false;
        }

        synchronized(mSendLock)
        {
            mSendChain = mSendChain.handle((ignored, error) -> null)
                .thenCompose(ignored -> webSocket.sendText(text, true).thenApply(sent -> null));
        }
        return true;
    }

    boolean trySend(ObjectNode body)
    {
        WebSocket webSocket = mWebSocket;
        if(!isReady() || webSocket == null)
        {
            return false;
        }

        String text;
        try
        {
            text = MAPPER.writeValueAsString(body);
        }
        catch(Exception e)
        {
            return false;
        }

        synchronized(mSendLock)
        {
            if(!mSendChain.isDone())
            {
                return false;
            }
            mSendChain = webSocket.sendText(text, true).thenApply(sent -> null);
        }
        return true;
    }

    private void closeSocket()
    {
        mReady.set(false);
        mConnecting = false;
        WebSocket webSocket = mWebSocket;
        mWebSocket = null;
        if(webSocket != null)
        {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "bye");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket)
    {
        mWebSocket = webSocket;
        webSocket.request(1);

        ObjectNode hello = MAPPER.createObjectNode();
        hello.put("type", "hello");
        hello.put("key", mApiKey);
        try
        {
            String text = MAPPER.writeValueAsString(hello);
            synchronized(mSendLock)
            {
                mSendChain = CompletableFuture.completedFuture(null)
                    .thenCompose(ignored -> webSocket.sendText(text, true).thenApply(sent -> null));
            }
        }
        catch(Exception e)
        {
            mLog.warn("Thinline Radio websocket hello failed: {}", e.toString());
            webSocket.abort();
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last)
    {
        mText.append(data);
        if(last)
        {
            String message = mText.toString();
            mText.setLength(0);
            handle(message);
        }
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason)
    {
        disconnected(webSocket);
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error)
    {
        mLog.warn("Thinline Radio websocket error: {}", error.toString());
        disconnected(webSocket);
    }

    private void disconnected(WebSocket webSocket)
    {
        mReady.set(false);
        if(mWebSocket == webSocket)
        {
            mWebSocket = null;
        }
        for(Runnable listener: mClosedListeners)
        {
            listener.run();
        }
    }

    private void handle(String message)
    {
        try
        {
            JsonNode node = MAPPER.readTree(message);
            String type = node.path("type").asText("");
            if("welcome".equals(type))
            {
                mReady.set(true);
                for(Runnable listener: mReadyListeners)
                {
                    listener.run();
                }
                return;
            }
            if("command".equals(type))
            {
                for(Consumer<JsonNode> listener: mCommandListeners)
                {
                    listener.accept(node);
                }
            }
        }
        catch(Exception e)
        {
            mLog.debug("Ignoring Thinline Radio websocket message", e);
        }
    }
}
