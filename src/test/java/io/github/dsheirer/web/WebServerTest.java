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

package io.github.dsheirer.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseConfiguration;
import io.github.dsheirer.database.DatabaseType;
import io.github.dsheirer.settings.web.WebServerSettings;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.PasswordHasher;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the web server on a random loopback port against an H2 database and exercises the API over HTTP: problem
 * responses, sign-in, the pending password change, CSRF, permissions, API tokens and the static client.
 */
public class WebServerTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @TempDir
    Path mTempDir;

    private CallDatabase mDatabase;
    private WebServer mServer;
    private String mBase;

    @BeforeEach
    void setUp() throws Exception
    {
        mDatabase = new CallDatabase(new DatabaseConfiguration(DatabaseType.H2, "", 0, "", "web_server_test", "",
            ""), mTempDir);
        mDatabase.install();
        mServer = new WebServer(new TestSettings(), null, () -> mDatabase, new PasswordHasher(1000));
        //Only the event source: the tests publish events to the live hub directly
        mServer.setSources(new WebSources(null, null, null, new io.github.dsheirer.event.EventManager(
            new io.github.dsheirer.settings.identifier.TalkgroupFormatSettings(null), null), null, null));
        mServer.start();
        assertEquals(WebServerStatus.State.RUNNING, mServer.getStatus().state(), mServer.getStatus().error());
        mBase = "http://127.0.0.1:" + mServer.getPort();
    }

    @AfterEach
    void tearDown()
    {
        mServer.stop();
        mDatabase.close();
    }

    /**
     * Browser-like client with a cookie jar and the CSRF token from the last sign-in.
     */
    private class Client
    {
        private final HttpClient mHttp = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
        private String mCsrf;
        private String mBearer;

        Response send(String method, String path, Object body) throws Exception
        {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(mBase + path));

            if(body != null)
            {
                builder.header("Content-Type", "application/json");
                builder.method(method, HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(body)));
            }
            else
            {
                builder.method(method, HttpRequest.BodyPublishers.noBody());
            }

            if(mCsrf != null)
            {
                builder.header(AccessManager.CSRF_HEADER, mCsrf);
            }

            if(mBearer != null)
            {
                builder.header("Authorization", "Bearer " + mBearer);
            }

            HttpResponse<String> response = mHttp.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            JsonNode json = response.body().isEmpty() || !response.headers().firstValue("Content-Type").orElse("")
                .contains("json") ? null : MAPPER.readTree(response.body());
            return new Response(response.statusCode(), json, response);
        }

        Response login(String username, String password) throws Exception
        {
            Response response = send("POST", "/api/v1/auth/login", new Login(username, password));

            if(response.status() == 200)
            {
                mCsrf = response.json().get("csrfToken").asText();
            }

            return response;
        }
    }

    record Login(String username, String password) {}

    record PasswordChange(String currentPassword, String newPassword) {}

    record NewUser(String username, String displayName, String role) {}

    record NewToken(String name, Integer expiresInDays, java.util.List<String> permissions) {}

    record Response(int status, JsonNode json, HttpResponse<String> raw)
    {
        String contentType()
        {
            return raw.headers().firstValue("Content-Type").orElse("");
        }
    }

    private Client signedInAdmin() throws Exception
    {
        String password = mServer.getAccountService().getPendingInitialPassword().orElseThrow();
        Client client = new Client();
        assertEquals(200, client.login("admin", password).status());
        assertEquals(204, client.send("POST", "/api/v1/auth/password",
            new PasswordChange(password, "admin password 1")).status());
        return client;
    }

    @Test
    void unauthenticatedRequestsGetProblemDetails() throws Exception
    {
        Response response = new Client().send("GET", "/api/v1/auth/me", null);
        assertEquals(401, response.status());
        assertTrue(response.contentType().startsWith("application/problem+json"));
        assertEquals(401, response.json().get("status").asInt());
        assertNotNull(response.json().get("title"));
        assertEquals("no-store", response.raw().headers().firstValue("Cache-Control").orElse(null));
        assertEquals("nosniff", response.raw().headers().firstValue("X-Content-Type-Options").orElse(null));

        Response missing = new Client().send("GET", "/api/v1/no-such-thing", null);
        assertEquals(404, missing.status());
        assertTrue(missing.contentType().startsWith("application/problem+json"));
    }

    @Test
    void openApiDocumentAndClientArePublic() throws Exception
    {
        Response openApi = new Client().send("GET", "/api/v1/openapi.json", null);
        assertEquals(200, openApi.status());
        assertTrue(openApi.json().get("paths").has("/api/v1/auth/login"));

        Response index = new Client().send("GET", "/", null);
        assertEquals(200, index.status());
        assertTrue(index.raw().body().contains("app.js"));
        assertTrue(index.raw().headers().firstValue("Content-Security-Policy").orElse("").contains("'self'"));
        assertFalse(index.raw().headers().firstValue("Content-Security-Policy").orElse("").contains("unsafe-inline"));

        //API reference page and its Swagger UI assets
        for(String path: new String[]{"/api/", "/api"})
        {
            Response reference = new Client().send("GET", path, null);
            assertEquals(200, reference.status(), path);
            assertTrue(reference.raw().body().contains("/api/swagger-ui/swagger-ui-bundle.js"), path);
            assertTrue(reference.raw().headers().firstValue("Content-Security-Policy").orElse("")
                .contains("style-src 'self' 'unsafe-inline'"), path);
        }

        assertEquals(200, new Client().send("GET", "/api/swagger-ui/swagger-ui-bundle.js", null).status());
        assertEquals(200, new Client().send("GET", "/js/app.js", null).status());
        assertEquals(200, new Client().send("GET", "/themes.css", null).status());
        Response icon = new Client().send("GET", "/images/sdrtrunk/sdrtrunk-icon-32.png", null);
        assertEquals(200, icon.status());
        assertEquals("image/png", icon.contentType());
    }

    @Test
    void temporaryPasswordMustBeChangedFirst() throws Exception
    {
        String password = mServer.getAccountService().getPendingInitialPassword().orElseThrow();
        Client client = new Client();
        Response login = client.login("admin", password);
        assertEquals(200, login.status());
        assertTrue(login.json().get("mustChangePassword").asBoolean());
        assertTrue(login.raw().headers().firstValue("Set-Cookie").orElse("").contains("HttpOnly"));
        assertTrue(login.raw().headers().firstValue("Set-Cookie").orElse("").contains("SameSite=Strict"));

        assertEquals(200, client.send("GET", "/api/v1/auth/me", null).status());
        assertEquals(403, client.send("GET", "/api/v1/system", null).status());

        assertEquals(204, client.send("POST", "/api/v1/auth/password",
            new PasswordChange(password, "admin password 1")).status());
        Response system = client.send("GET", "/api/v1/system", null);
        assertEquals(200, system.status());
        assertNotNull(system.json().get("instanceId").asText());
    }

    @Test
    void wrongPasswordIsRefused() throws Exception
    {
        mServer.getAccountService().getPendingInitialPassword().orElseThrow();
        Response response = new Client().login("admin", "not the password");
        assertEquals(401, response.status());
        assertTrue(response.contentType().startsWith("application/problem+json"));
    }

    @Test
    void stateChangingRequestsNeedTheCsrfToken() throws Exception
    {
        Client client = signedInAdmin();
        client.mCsrf = null;
        Response response = client.send("POST", "/api/v1/users", new NewUser("op", null, "OPERATOR"));
        assertEquals(403, response.status());
        assertTrue(response.json().get("detail").asText().contains("CSRF"));
    }

    @Test
    void requestBodiesMustBeJson() throws Exception
    {
        Client client = signedInAdmin();
        HttpRequest request = HttpRequest.newBuilder(URI.create(mBase + "/api/v1/users"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header(AccessManager.CSRF_HEADER, client.mCsrf)
            .POST(HttpRequest.BodyPublishers.ofString("username=op&role=OPERATOR")).build();
        assertEquals(415, client.mHttp.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());

        Response unknownField = client.send("POST", "/api/v1/users", java.util.Map.of("username", "op",
            "role", "OPERATOR", "colour", "red"));
        assertEquals(400, unknownField.status());
        assertTrue(unknownField.json().get("detail").asText().contains("colour"));
    }

    @Test
    void permissionsAreEnforced() throws Exception
    {
        Client admin = signedInAdmin();
        Response created = admin.send("POST", "/api/v1/users", new NewUser("viewer", "Wall Display", "VIEWER"));
        assertEquals(201, created.status());
        String temporary = created.json().get("temporaryPassword").asText();

        Client viewer = new Client();
        assertEquals(200, viewer.login("viewer", temporary).status());
        assertEquals(204, viewer.send("POST", "/api/v1/auth/password",
            new PasswordChange(temporary, "viewer password")).status());

        Response denied = viewer.send("GET", "/api/v1/users", null);
        assertEquals(403, denied.status());
        assertEquals("users.manage required", denied.json().get("detail").asText());
        assertEquals(200, viewer.send("GET", "/api/v1/system", null).status());

        Response users = admin.send("GET", "/api/v1/users?limit=1", null);
        assertEquals(200, users.status());
        assertEquals(1, users.json().get("items").size());
        String cursor = users.json().get("nextCursor").asText();
        Response next = admin.send("GET", "/api/v1/users?limit=1&cursor=" + cursor, null);
        assertEquals("viewer", next.json().get("items").get(0).get("username").asText());
        assertTrue(next.json().get("nextCursor").isNull());

        Response audit = admin.send("GET", "/api/v1/audit?action=access.denied", null);
        assertEquals(200, audit.status());
        assertEquals("viewer", audit.json().get("items").get(0).get("username").asText());
    }

    @Test
    void apiTokensAuthenticateWithoutCsrf() throws Exception
    {
        Client admin = signedInAdmin();
        Response created = admin.send("POST", "/api/v1/auth/tokens", new NewToken("script", 30,
            java.util.List.of("events.read")));
        assertEquals(201, created.status());
        String token = created.json().get("token").asText();
        assertFalse(created.json().get("details").has("token"));

        Client script = new Client();
        script.mBearer = token;
        Response me = script.send("GET", "/api/v1/auth/me", null);
        assertEquals(200, me.status());
        assertEquals("TOKEN", me.json().get("authMethod").asText());
        assertEquals(403, script.send("GET", "/api/v1/users", null).status());

        script.mBearer = "sdrt_wrong";
        assertEquals(401, script.send("GET", "/api/v1/auth/me", null).status());
    }

    @Test
    void logoutEndsTheSession() throws Exception
    {
        Client admin = signedInAdmin();
        assertEquals(204, admin.send("POST", "/api/v1/auth/logout", null).status());
        assertEquals(401, admin.send("GET", "/api/v1/auth/me", null).status());
    }

    @Test
    void storedCallsAndEvents() throws Exception
    {
        try(java.sql.Connection connection = mDatabase.getConnection())
        {
            for(int x = 0; x < 3; x++)
            {
                io.github.dsheirer.database.call.CallRecord call = new io.github.dsheirer.database.call.CallRecord();
                call.setCallKey(100 + x);
                call.setStartTime(1_800_000_000_000L + x * 1000);
                call.setEndTime(1_800_000_000_500L + x * 1000);
                call.setCallState(io.github.dsheirer.database.call.CallState.COMPLETE);
                call.setProtocol("APCO25");
                call.setSystem("County");
                call.setToId(x == 2 ? "2000" : "1201");
                call.setToValue(x == 2 ? 2000L : 1201L);
                call.setToForm("TALKGROUP");
                mDatabase.getCallRepository().insert(connection, call);
            }

            io.github.dsheirer.database.event.EventRecord event = new io.github.dsheirer.database.event.EventRecord();
            event.setEventKey(1);
            event.setEventType("REGISTER");
            event.setStartTime(1_800_000_000_000L);
            event.setDetails("Registered");
            mDatabase.getEventRepository().insert(connection, event);
        }

        Client client = signedInAdmin();
        Response all = client.send("GET", "/api/v1/calls?limit=2", null);
        assertEquals(200, all.status());
        assertEquals(2, all.json().get("items").size());
        assertEquals("2000", all.json().get("items").get(0).get("to").get("formatted").asText(), "newest first");
        assertEquals("2027-01-15T08:00:02Z", all.json().get("items").get(0).get("start").asText());
        Response next = client.send("GET", "/api/v1/calls?limit=2&cursor=" +
            all.json().get("nextCursor").asText(), null);
        assertEquals(1, next.json().get("items").size());
        assertTrue(next.json().get("nextCursor").isNull());

        Response filtered = client.send("GET", "/api/v1/calls?to=1201&since=2027-01-15T08:00:00.500Z", null);
        assertEquals(1, filtered.json().get("items").size());

        long id = filtered.json().get("items").get(0).get("id").asLong();
        assertEquals(1201, client.send("GET", "/api/v1/calls/" + id, null).json().get("to").get("value").asInt());
        assertEquals(404, client.send("GET", "/api/v1/calls/999999", null).status());

        Response invalid = client.send("GET", "/api/v1/calls?protocol=NOPE&since=yesterday", null);
        assertEquals(400, invalid.status());
        assertEquals(2, invalid.json().get("errors").size());

        Response events = client.send("GET", "/api/v1/events?category=REGISTRATION", null);
        assertEquals(1, events.json().get("items").size());
        assertEquals("Registered", events.json().get("items").get(0).get("details").asText());
    }

    @Test
    void exactTalkgroupFilter() throws Exception
    {
        try(java.sql.Connection connection = mDatabase.getConnection())
        {
            long[] talkgroups = {1203, 11203, 12030, 1201};

            for(int x = 0; x < talkgroups.length; x++)
            {
                io.github.dsheirer.database.call.CallRecord call = new io.github.dsheirer.database.call.CallRecord();
                call.setStartTime(1_800_000_000_000L + x * 1000);
                call.setCallState(io.github.dsheirer.database.call.CallState.COMPLETE);
                call.setProtocol("APCO25");
                call.setToId(String.valueOf(talkgroups[x]));
                call.setToValue(talkgroups[x]);
                call.setToForm("TALKGROUP");

                if(talkgroups[x] == 1201)
                {
                    //Patch group 1201 with 1203 patched in
                    call.setToForm("PATCH_GROUP");
                    call.setPatchedTalkgroups(java.util.List.of(
                        new io.github.dsheirer.database.call.PatchedTalkgroup(1203, null)));
                }

                mDatabase.getCallRepository().insert(connection, call);
            }
        }

        Client client = signedInAdmin();
        Response text = client.send("GET", "/api/v1/calls?to=1203", null);
        assertEquals(4, text.json().get("items").size(), "text matching also finds 11203 and 12030");

        Response exact = client.send("GET", "/api/v1/calls?toValue=1203", null);
        java.util.List<String> found = new java.util.ArrayList<>();
        exact.json().get("items").forEach(item -> found.add(item.get("to").get("formatted").asText()));
        assertEquals(java.util.List.of("1201", "1203"), found, "1203 itself and the patch group it is in");

        assertEquals(400, client.send("GET", "/api/v1/calls?toValue=abc", null).status());
        assertEquals(400, client.send("GET", "/api/v1/calls?toValue=1203&to=1203", null).status());

        //The same filter on live calls
        io.github.dsheirer.database.call.CallSearchCriteria criteria = io.github.dsheirer.web.api.RecordFilters
            .calls(name -> "toValue".equals(name) ? "1203" : null);
        io.github.dsheirer.database.call.CallRecord live = new io.github.dsheirer.database.call.CallRecord();
        live.setToId("11203");
        live.setToValue(11203L);
        live.setToForm("TALKGROUP");
        assertFalse(criteria.matches(live));
        live.setToValue(1203L);
        assertTrue(criteria.matches(live));
    }

    @Test
    void unavailableSourcesReport503() throws Exception
    {
        Client client = signedInAdmin();
        assertEquals(503, client.send("GET", "/api/v1/tuners", null).status());
        assertEquals(503, client.send("GET", "/api/v1/channels", null).status());
    }

    @Test
    void webSocketWithTicket() throws Exception
    {
        Client client = signedInAdmin();
        String ticket = client.send("POST", "/api/v1/auth/ws-ticket", null).json().get("ticket").asText();

        java.util.concurrent.LinkedBlockingQueue<JsonNode> received = new java.util.concurrent.LinkedBlockingQueue<>();
        java.net.http.WebSocket socket = HttpClient.newHttpClient().newWebSocketBuilder()
            .buildAsync(URI.create("ws://127.0.0.1:" + mServer.getPort() + "/api/v1/ws?ticket=" + ticket),
                new java.net.http.WebSocket.Listener()
                {
                    private final StringBuilder mText = new StringBuilder();

                    @Override
                    public java.util.concurrent.CompletionStage<?> onText(java.net.http.WebSocket webSocket,
                                                                          CharSequence data, boolean last)
                    {
                        mText.append(data);

                        if(last)
                        {
                            try
                            {
                                received.add(MAPPER.readTree(mText.toString()));
                            }
                            catch(Exception e)
                            {
                                throw new IllegalStateException(e);
                            }

                            mText.setLength(0);
                        }

                        webSocket.request(1);
                        return null;
                    }
                }).get(5, java.util.concurrent.TimeUnit.SECONDS);

        socket.sendText("{\"op\":\"subscribe\",\"sub\":5,\"topic\":\"events\"}", true);
        JsonNode subscribed = received.poll(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(subscribed);
        assertEquals("subscribed", subscribed.get("op").asText());

        io.github.dsheirer.database.event.EventRecord event = new io.github.dsheirer.database.event.EventRecord();
        event.setEventKey(42);
        event.setEventType("REGISTER");
        mServer.getLiveHub().eventChanged(event);

        JsonNode data = received.poll(5, java.util.concurrent.TimeUnit.SECONDS);
        assertNotNull(data);
        assertEquals("event.created", data.path("data").path("type").asText());
        assertEquals(42, data.path("data").path("event").path("eventKey").asInt());

        //The calls topic has no source in this test
        socket.sendText("{\"op\":\"subscribe\",\"sub\":6,\"topic\":\"calls\"}", true);
        assertEquals(503, received.poll(5, java.util.concurrent.TimeUnit.SECONDS).get("status").asInt());
        socket.sendClose(1000, "done").get(5, java.util.concurrent.TimeUnit.SECONDS);

        //A ticket is good once
        java.util.concurrent.CompletableFuture<java.net.http.WebSocket> reused = HttpClient.newHttpClient()
            .newWebSocketBuilder().buildAsync(URI.create("ws://127.0.0.1:" + mServer.getPort() + "/api/v1/ws?ticket=" +
                ticket), new java.net.http.WebSocket.Listener() {});
        assertThrows(java.util.concurrent.ExecutionException.class, () ->
            reused.get(5, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Test
    void webSocketCookieFromAnotherOriginIsRefused() throws Exception
    {
        Client client = signedInAdmin();
        String cookie = ((CookieManager)client.mHttp.cookieHandler().orElseThrow()).getCookieStore().getCookies()
            .stream().filter(c -> c.getName().equals(AccessManager.SESSION_COOKIE)).findFirst().orElseThrow()
            .toString();

        java.util.concurrent.CompletableFuture<java.net.http.WebSocket> foreign = HttpClient.newHttpClient()
            .newWebSocketBuilder().header("Cookie", cookie).header("Origin", "https://evil.example")
            .buildAsync(URI.create("ws://127.0.0.1:" + mServer.getPort() + "/api/v1/ws"),
                new java.net.http.WebSocket.Listener() {});
        assertThrows(java.util.concurrent.ExecutionException.class, () ->
            foreign.get(5, java.util.concurrent.TimeUnit.SECONDS));

        java.util.concurrent.CompletableFuture<java.net.http.WebSocket> same = HttpClient.newHttpClient()
            .newWebSocketBuilder().header("Cookie", cookie).header("Origin", mBase)
            .buildAsync(URI.create("ws://127.0.0.1:" + mServer.getPort() + "/api/v1/ws"),
                new java.net.http.WebSocket.Listener() {});
        same.get(5, java.util.concurrent.TimeUnit.SECONDS).sendClose(1000, "done");
    }

    @Test
    void httpsWithAKeystore() throws Exception
    {
        Path keystore = mTempDir.resolve("server.p12");
        Process keytool = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "keytool").toString(),
            "-genkeypair", "-alias", "server", "-keyalg", "EC", "-groupname", "secp256r1", "-dname", "CN=localhost",
            "-ext", "SAN=ip:127.0.0.1", "-validity", "2", "-storetype", "PKCS12", "-keystore", keystore.toString(), "-storepass", "changeit")
            .redirectErrorStream(true).start();
        assertEquals(0, keytool.waitFor(), new String(keytool.getInputStream().readAllBytes()));

        WebServer server = new WebServer(new TestSettings(keystore.toString(), "changeit"), null, () -> mDatabase,
            new PasswordHasher(1000));
        server.start();

        try
        {
            assertEquals(WebServerStatus.State.RUNNING, server.getStatus().state(), server.getStatus().error());
            assertTrue(server.getStatus().url().startsWith("https://"));

            javax.net.ssl.SSLContext ssl = javax.net.ssl.SSLContext.getInstance("TLS");
            ssl.init(null, new javax.net.ssl.TrustManager[]{new javax.net.ssl.X509TrustManager()
            {
                public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String type) {}
                public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String type) {}
                public java.security.cert.X509Certificate[] getAcceptedIssuers() { return new java.security.cert.X509Certificate[0]; }
            }}, null);
            HttpClient client = HttpClient.newBuilder().sslContext(ssl).build();
            String password = server.getAccountService().getPendingInitialPassword()
                .orElseGet(() -> mServer.getAccountService().getPendingInitialPassword().orElseThrow());
            HttpResponse<String> login = client.send(HttpRequest.newBuilder(URI.create("https://127.0.0.1:" +
                server.getPort() + "/api/v1/auth/login")).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(MAPPER.writeValueAsString(new Login("admin", password))))
                .build(), HttpResponse.BodyHandlers.ofString());
            assertEquals(200, login.statusCode());
            assertTrue(login.headers().firstValue("Set-Cookie").orElse("").contains("Secure"));
            assertTrue(login.headers().firstValue("Strict-Transport-Security").isPresent());
        }
        finally
        {
            server.stop();
        }
    }

    @Test
    void missingKeystoreFailsToStart()
    {
        WebServer server = new WebServer(new TestSettings(mTempDir.resolve("missing.p12").toString(), "x"), null,
            () -> mDatabase, new PasswordHasher(1000));
        server.start();

        try
        {
            assertEquals(WebServerStatus.State.FAILED, server.getStatus().state());
            assertTrue(server.getStatus().error().contains("keystore"));
        }
        finally
        {
            server.stop();
        }
    }

    /**
     * Settings that don't read or write the Java preferences store
     */
    private static class TestSettings extends WebServerSettings
    {
        private final String mKeystore;
        private final String mKeystorePassword;

        TestSettings()
        {
            this(null, null);
        }

        /**
         * HTTPS with the keystore, or HTTP when the keystore is null
         */
        TestSettings(String keystore, String keystorePassword)
        {
            super(null);
            mKeystore = keystore;
            mKeystorePassword = keystorePassword;
        }

        @Override
        public String getTlsKeystorePath()
        {
            return mKeystore;
        }

        @Override
        public String getTlsKeystorePassword()
        {
            return mKeystorePassword;
        }

        @Override
        public boolean isEnabled()
        {
            return true;
        }

        @Override
        public String getBindAddress()
        {
            return "127.0.0.1";
        }

        @Override
        public int getPort()
        {
            return 0;
        }

        @Override
        public boolean isTlsEnabled()
        {
            return mKeystore != null;
        }

        @Override
        public int getSessionTimeoutMinutes()
        {
            return 60;
        }

        @Override
        public String getInstanceId()
        {
            return "00000000-0000-0000-0000-000000000001";
        }
    }
}
