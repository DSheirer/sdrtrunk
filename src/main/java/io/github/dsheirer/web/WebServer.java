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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseEvent;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.web.WebServerSettings;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.web.account.AccountService;
import io.github.dsheirer.web.api.ApiException;
import io.github.dsheirer.web.api.ApiSupport;
import io.github.dsheirer.web.api.AuthController;
import io.github.dsheirer.web.api.ChannelsController;
import io.github.dsheirer.web.api.PlaylistController;
import io.github.dsheirer.web.api.RecordsController;
import io.github.dsheirer.web.api.TunersController;
import io.github.dsheirer.web.api.SystemController;
import io.github.dsheirer.web.api.UserController;
import io.github.dsheirer.web.auth.AccessManager;
import io.github.dsheirer.web.auth.PasswordHasher;
import io.github.dsheirer.web.auth.Permission;
import io.github.dsheirer.web.auth.SessionManager;
import io.github.dsheirer.web.live.LiveAudio;
import io.github.dsheirer.web.live.LiveEndpoint;
import io.github.dsheirer.web.live.LiveHub;
import io.github.dsheirer.web.playlist.AliasRegistry;
import io.github.dsheirer.web.playlist.PlaylistService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpResponseException;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import io.javalin.openapi.CookieAuth;
import io.javalin.openapi.plugin.OpenApiPlugin;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Embedded web server for the web API (Javalin on Jetty 12).  Off by default; enabled and configured in the Web
 * Server settings, or with the launcher options for a headless install.
 *
 * The web layer is a thin adapter: handlers parse the request, call a service class and serialize the result, so a
 * change of web framework touches only the handlers.
 *
 * A settings change restarts the server a moment later, so a burst of edits causes one restart.  Browser sessions
 * are held in memory and survive a server restart, but not an application restart.
 */
public class WebServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WebServer.class);
    private static final long RESTART_DELAY_MS = 1500;
    private static final long MAX_REQUEST_BYTES = 1024 * 1024;
    private static final String SWAGGER_UI_VERSION = "5.34.1";
    private static final String SWAGGER_UI_PATH = "/api/swagger-ui";
    private static final String CSP = "default-src 'self'; frame-ancestors 'none'";
    //Swagger UI sets inline styles and shows inline SVG images, so the API reference page allows those
    private static final String CSP_API_REFERENCE = "default-src 'self'; style-src 'self' 'unsafe-inline'; " +
        "img-src 'self' data:; frame-ancestors 'none'";

    private final WebServerSettings mSettings;
    private final DatabaseManager mDatabaseManager;
    private final SessionManager mSessionManager;
    private final AccountService mAccountService;
    private final Supplier<CallDatabase> mDatabase;
    private final ObjectMapper mMapper = new ObjectMapper().disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
    private final Broadcaster<WebServerStatus> mStatusBroadcaster = new Broadcaster<>();
    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor(
        new NamingThreadFactory("sdrtrunk web server"));
    private final Listener<DatabaseEvent> mDatabaseListener = this::databaseChanged;
    private volatile WebServerStatus mStatus = WebServerStatus.stopped();
    private volatile boolean mTlsActive;
    private volatile String mDatabaseIdentity;
    private Javalin mJavalin;
    private ScheduledFuture<?> mRestartFuture;
    private ScheduledFuture<?> mPurgeFuture;
    private ScheduledFuture<?> mRefreshFuture;
    private volatile WebSources mSources = WebSources.none();
    private volatile LiveHub mLiveHub;
    private LiveAudio mLiveAudio;
    private final AliasRegistry mAliasRegistry = new AliasRegistry();

    /**
     * Constructs an instance
     * @param settings for the web server
     * @param databaseManager for the accounts, stored in the call database
     */
    public WebServer(WebServerSettings settings, DatabaseManager databaseManager)
    {
        this(settings, databaseManager, databaseManager::getDatabase, new PasswordHasher());
    }

    /**
     * Constructs an instance with a database supplier and password hasher, for tests.
     */
    WebServer(WebServerSettings settings, DatabaseManager databaseManager, Supplier<CallDatabase> database,
              PasswordHasher hasher)
    {
        mSettings = settings;
        mDatabaseManager = databaseManager;
        mDatabase = database;
        mSessionManager = new SessionManager(settings::getSessionTimeoutMinutes, System::currentTimeMillis);
        mAccountService = new AccountService(database, hasher, mSessionManager, System::currentTimeMillis,
            settings::isRegistrationEnabled);
    }

    /**
     * Sets the application components that the API reads from.  Call before {@link #start()}.
     */
    public void setSources(WebSources sources)
    {
        mSources = sources != null ? sources : WebSources.none();

        //Live audio listens to the channels for the life of the application: audio segment listeners attach only to
        //channels that start after them, so it is registered before any channel starts
        if(mLiveAudio == null && mSources.playlistManager() != null)
        {
            mLiveAudio = new LiveAudio(() -> mLiveHub);
            mSources.playlistManager().getChannelProcessingManager().addAudioSegmentListener(mLiveAudio);
            mLiveAudio.start();
        }
    }

    /**
     * Live update hub while the server runs, or null
     */
    LiveHub getLiveHub()
    {
        return mLiveHub;
    }

    /**
     * Starts the server when it is enabled, and follows settings and database changes from then on.
     */
    public void start()
    {
        MyEventBus.getGlobalEventBus().register(this);

        if(mDatabaseManager != null)
        {
            mDatabaseManager.addListener(mDatabaseListener);
        }

        mPurgeFuture = mExecutor.scheduleWithFixedDelay(mSessionManager::purgeExpired, 5, 5, TimeUnit.MINUTES);
        mRefreshFuture = mExecutor.scheduleWithFixedDelay(this::refreshLiveConnections, 60, 60, TimeUnit.SECONDS);
        runAndWait(this::restartNow);
    }

    /**
     * Stops the server.  Called at application shutdown.
     */
    public void stop()
    {
        MyEventBus.getGlobalEventBus().unregister(this);

        if(mDatabaseManager != null)
        {
            mDatabaseManager.removeListener(mDatabaseListener);
        }

        if(mPurgeFuture != null)
        {
            mPurgeFuture.cancel(false);
        }

        if(mRefreshFuture != null)
        {
            mRefreshFuture.cancel(false);
        }

        runAndWait(this::stopNow);
        mExecutor.shutdownNow();

        if(mLiveAudio != null)
        {
            mSources.playlistManager().getChannelProcessingManager().removeAudioSegmentListener(mLiveAudio);
            mLiveAudio.stop();
            mLiveAudio = null;
        }
    }

    /**
     * Current state of the server
     */
    public WebServerStatus getStatus()
    {
        return mStatus;
    }

    /**
     * Registers a listener for server state changes.  Notifications arrive on the web server thread.
     */
    public void addStatusListener(Listener<WebServerStatus> listener)
    {
        mStatusBroadcaster.addListener(listener);
    }

    public void removeStatusListener(Listener<WebServerStatus> listener)
    {
        mStatusBroadcaster.removeListener(listener);
    }

    /**
     * Account service, for the settings dialog (initial administrator password).
     */
    public AccountService getAccountService()
    {
        return mAccountService;
    }

    /**
     * Settings change notification from the global event bus.
     */
    @Subscribe
    public void settingsChanged(SettingsType type)
    {
        if(type == SettingsType.WEB)
        {
            mExecutor.execute(() -> {
                if(mRestartFuture != null)
                {
                    mRestartFuture.cancel(false);
                }

                mRestartFuture = mExecutor.schedule(this::restartNow, RESTART_DELAY_MS, TimeUnit.MILLISECONDS);
            });
        }
    }

    /**
     * Call database opened or switched: create the initial administrator in an empty database, and end the sessions
     * when a different database (with different accounts) was selected.
     */
    private void databaseChanged(DatabaseEvent event)
    {
        if(event.type() == DatabaseEvent.Type.CONNECTED)
        {
            mExecutor.execute(() -> {
                String identity = event.configuration() != null ? event.configuration().getDescription() + "/" +
                    event.configuration().schema() : null;

                if(mDatabaseIdentity != null && !mDatabaseIdentity.equals(identity))
                {
                    LOGGER.info("Web API: a different database was selected - signing out all web sessions");
                    mSessionManager.invalidateAll();
                    mAccountService.clearActivity();
                }

                mDatabaseIdentity = identity;

                if(mJavalin != null)
                {
                    mAccountService.ensureInitialAdmin();
                }
            });
        }
    }

    private void restartNow()
    {
        stopNow();

        if(!mSettings.isEnabled())
        {
            setStatus(WebServerStatus.stopped());
            return;
        }

        String host = mSettings.getBindAddress();
        int port = mSettings.getPort();
        boolean tls = mSettings.isTlsEnabled();

        try
        {
            if(tls)
            {
                checkKeystore();
            }

            mTlsActive = tls;
            mJavalin = createJavalin(host, port, tls).start();
            int actualPort = mJavalin.port();
            String url = (tls ? "https://" : "http://") + displayHost(host) + ":" + actualPort + "/";
            LOGGER.info("Web API listening on " + host + ":" + actualPort + (tls ? " (HTTPS)" : " (HTTP)") +
                " - " + url);

            if(!tls && !isLoopback(host))
            {
                LOGGER.warn("Web API is reachable from the network without TLS.  Passwords and session cookies are " +
                    "sent in plain text: turn on TLS, or use a reverse proxy with TLS.");
            }

            mDatabaseIdentity = mDatabaseManager != null ? describe(mDatabaseManager.getDatabase()) : null;
            mAccountService.ensureInitialAdmin();
            setStatus(new WebServerStatus(WebServerStatus.State.RUNNING, url, null));
        }
        catch(Exception e)
        {
            stopNow();
            String message = describeStartError(e, host, port);
            LOGGER.error("Web API couldn't start: " + message, e);
            setStatus(new WebServerStatus(WebServerStatus.State.FAILED, null, message));
        }
    }

    /**
     * Checks the callers of open WebSocket connections again.  Web server thread.
     */
    void refreshLiveConnections()
    {
        LiveHub hub = mLiveHub;

        if(hub != null)
        {
            try
            {
                hub.refreshPrincipals();
            }
            catch(Exception e)
            {
                LOGGER.error("Error checking web API WebSocket connections", e);
            }
        }
    }

    private void stopNow()
    {
        if(mLiveHub != null)
        {
            mLiveHub.detach();
            mLiveHub = null;
        }

        if(mJavalin != null)
        {
            try
            {
                mJavalin.stop();
                LOGGER.info("Web API stopped");
            }
            catch(Exception e)
            {
                LOGGER.error("Error stopping the web API server", e);
            }

            mJavalin = null;
        }
    }

    /**
     * Port the server is listening on, or -1 when it isn't running.  Useful when the port setting is 0 (tests).
     */
    int getPort()
    {
        Javalin javalin = mJavalin;
        return javalin != null ? javalin.port() : -1;
    }

    private Javalin createJavalin(String host, int port, boolean tls)
    {
        AccessManager accessManager = new AccessManager(mAccountService, mSessionManager);
        AuthController auth = new AuthController(mAccountService, mSessionManager, () -> mTlsActive,
            System::currentTimeMillis);
        UserController users = new UserController(mAccountService, mSessionManager, System::currentTimeMillis);
        SystemController system = new SystemController(mSettings::getInstanceId, mDatabaseManager);
        WebSources sources = mSources;
        TunersController tuners = new TunersController(sources.tunerManager(), mAccountService);
        PlaylistService playlist = sources.playlistManager() == null ? null : new PlaylistService(
            sources.playlistManager().getChannelModel(), sources.playlistManager().getAliasModel(),
            sources.playlistManager().getChannelProcessingManager(), mAliasRegistry);
        PlaylistController playlistController = new PlaylistController(playlist, mAccountService);
        ChannelsController channels = new ChannelsController(sources.playlistManager(), sources.talkgroupFormat());
        RecordsController records = new RecordsController(mDatabase, sources.aliasModel());
        mLiveHub = new LiveHub(sources, mDatabaseManager, mAccountService, mMapper, mAliasRegistry);
        mLiveHub.attach();
        LiveEndpoint live = new LiveEndpoint(mLiveHub, mAccountService, mSessionManager, System::currentTimeMillis);
        String version = SystemController.class.getPackage().getImplementationVersion();

        return Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.concurrency.useVirtualThreads = true;
            config.jetty.host = host;
            config.jetty.port = port;
            config.http.maxRequestSize = MAX_REQUEST_BYTES;
            config.jsonMapper(new JavalinJackson(mMapper, true));
            config.jetty.modifyWebSocketServletFactory(factory -> {
                factory.setMaxTextMessageSize(64 * 1024);
                factory.setIdleTimeout(java.time.Duration.ofMinutes(5));
            });

            if(tls)
            {
                config.jetty.addConnector((server, httpConfiguration) -> createTlsConnector(server,
                    httpConfiguration, host, port));
            }

            //Swagger UI for the API reference page at /api
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = SWAGGER_UI_PATH;
                staticFiles.directory = "/META-INF/resources/webjars/swagger-ui/" + SWAGGER_UI_VERSION;
                staticFiles.location = Location.CLASSPATH;
                staticFiles.roles = Set.of(Permission.PUBLIC);
            });

            //The desktop application's icons, for the web client's tab icon and header
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/images/sdrtrunk";
                staticFiles.directory = "/images/sdrtrunk";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.roles = Set.of(Permission.PUBLIC);
            });

            //Bundled web client
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory = "/web";
                staticFiles.location = Location.CLASSPATH;
                staticFiles.roles = Set.of(Permission.PUBLIC);
            });

            config.registerPlugin(new OpenApiPlugin(openApi -> openApi
                .withDocumentationPath(ApiSupport.API_V1 + "/openapi.json")
                .withRoles(Permission.PUBLIC)
                .withDefinitionConfiguration((documentVersion, definition) -> definition
                    .info(info -> info.title("sdrtrunk Web API").version(version != null ? version : "development")
                        .description("Browser sessions use the session cookie and send the csrfToken from " +
                            "/api/v1/auth/me in the X-CSRF-Token header of state-changing requests.  Scripts use " +
                            "an API token as Authorization: Bearer."))
                    .withBearerAuth("bearer", bearer -> {})
                    .withSecurityScheme("session", new CookieAuth(AccessManager.SESSION_COOKIE)))));

            config.routes.before(this::addSecurityHeaders);
            config.routes.beforeMatched(accessManager);

            auth.register(config.routes);
            users.register(config.routes);
            system.register(config.routes);
            tuners.register(config.routes);
            channels.register(config.routes);
            playlistController.register(config.routes);
            records.register(config.routes);
            live.register(config.routes);

            config.routes.exception(ApiException.class, (e, ctx) ->
                problem(ctx, e.getStatus(), e.getTitle(), e.getDetail(), e.getErrors()));
            config.routes.exception(HttpResponseException.class, (e, ctx) ->
                problem(ctx, e.getStatus(), e.getMessage(), null, List.of()));
            config.routes.exception(Exception.class, (e, ctx) -> {
                LOGGER.error("Web API request failed: " + ctx.method() + " " + ctx.path(), e);
                problem(ctx, 500, "Server error", "The request failed.  See the application log.", List.of());
            });
            config.routes.error(404, ctx -> {
                if(ctx.path().startsWith(ApiSupport.API_V1 + "/") &&
                    !"application/problem+json".equals(ctx.res().getContentType()))
                {
                    problem(ctx, 404, "Not found", "No endpoint " + ctx.method() + " " + ctx.path(), List.of());
                }
            });
        });
    }

    private ServerConnector createTlsConnector(org.eclipse.jetty.server.Server server,
                                               HttpConfiguration httpConfiguration, String host, int port)
    {
        SslContextFactory.Server ssl = new SslContextFactory.Server();
        ssl.setKeyStorePath(mSettings.getTlsKeystorePath());
        ssl.setKeyStorePassword(mSettings.getTlsKeystorePassword());
        ssl.setKeyStoreType("PKCS12");

        HttpConfiguration https = new HttpConfiguration(httpConfiguration);
        SecureRequestCustomizer customizer = new SecureRequestCustomizer();
        //Allow certificates for an IP address or a name other than the request host, common on a LAN
        customizer.setSniHostCheck(false);
        https.addCustomizer(customizer);

        ServerConnector connector = new ServerConnector(server, new SslConnectionFactory(ssl, "http/1.1"),
            new HttpConnectionFactory(https));
        connector.setHost(host);
        connector.setPort(port);
        return connector;
    }

    private void checkKeystore()
    {
        String path = mSettings.getTlsKeystorePath();

        if(path == null || path.isBlank())
        {
            throw new IllegalStateException("TLS is on but no keystore file is set");
        }

        if(!Files.isReadable(Path.of(path)))
        {
            throw new IllegalStateException("TLS keystore file can't be read: " + path);
        }
    }

    private void addSecurityHeaders(Context ctx)
    {
        ctx.header("X-Content-Type-Options", "nosniff");
        ctx.header("X-Frame-Options", "DENY");
        ctx.header("Referrer-Policy", "no-referrer");
        String path = ctx.path();
        ctx.header("Content-Security-Policy", path.equals("/api") || path.equals("/api/") ||
            path.equals("/api/index.html") ? CSP_API_REFERENCE : CSP);

        if(mTlsActive)
        {
            ctx.header("Strict-Transport-Security", "max-age=31536000");
        }

        if(path.startsWith(ApiSupport.API_V1 + "/"))
        {
            ctx.header("Cache-Control", "no-store");
        }
    }

    /**
     * Writes an RFC 9457 problem details response.
     */
    private void problem(Context ctx, int status, String title, String detail, List<ApiException.FieldError> errors)
    {
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("type", "about:blank");
        body.put("title", title);
        body.put("status", status);

        if(detail != null)
        {
            body.put("detail", detail);
        }

        if(errors != null && !errors.isEmpty())
        {
            body.put("errors", errors);
        }

        try
        {
            //ctx.json() would replace the content type with application/json
            ctx.status(status).contentType("application/problem+json").result(mMapper.writeValueAsString(body));
        }
        catch(Exception e)
        {
            ctx.status(500).result("Server error");
        }
    }

    private void setStatus(WebServerStatus status)
    {
        mStatus = status;
        mStatusBroadcaster.receive(status);
    }

    private void runAndWait(Runnable runnable)
    {
        try
        {
            mExecutor.submit(runnable).get(30, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            LOGGER.error("Web API server start or stop didn't complete", e);
        }
    }

    private static String describe(CallDatabase database)
    {
        return database == null ? null : database.getConfiguration().getDescription() + "/" +
            database.getConfiguration().schema();
    }

    private static boolean isLoopback(String host)
    {
        try
        {
            return InetAddress.getByName(host).isLoopbackAddress();
        }
        catch(Exception e)
        {
            return false;
        }
    }

    private static String displayHost(String host)
    {
        if(WebServerSettings.ALL_ADDRESSES.equals(host) || "::".equals(host))
        {
            try
            {
                return InetAddress.getLocalHost().getHostName();
            }
            catch(Exception e)
            {
                return "localhost";
            }
        }

        return host.contains(":") ? "[" + host + "]" : host;
    }

    private static String describeStartError(Throwable e, String host, int port)
    {
        Throwable cause = e;

        while(cause != null)
        {
            if(cause instanceof java.net.BindException)
            {
                return "Port " + port + " on " + host + " is in use or not allowed (" + cause.getMessage() + ")";
            }

            cause = cause.getCause();
        }

        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
