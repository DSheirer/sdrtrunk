package io.github.dsheirer.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import io.github.dsheirer.audio.broadcast.AbstractAudioBroadcaster;
import io.github.dsheirer.audio.broadcast.BroadcastConfiguration;
import io.github.dsheirer.audio.broadcast.BroadcastFactory;
import io.github.dsheirer.audio.broadcast.BroadcastFormat;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyCallConfiguration;
import io.github.dsheirer.audio.broadcast.broadcastify.BroadcastifyFeedConfiguration;
import io.github.dsheirer.audio.broadcast.broadcastify.TalkerAliasSource;
import io.github.dsheirer.audio.broadcast.icecast.IcecastConfiguration;
import io.github.dsheirer.audio.broadcast.openmhz.OpenMHzConfiguration;
import io.github.dsheirer.audio.broadcast.remote.RemoteApiBroadcaster;
import io.github.dsheirer.audio.broadcast.remote.RemoteApiConfiguration;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerBroadcaster;
import io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration;
import io.github.dsheirer.audio.broadcast.shoutcast.v1.ShoutcastV1Configuration;
import io.github.dsheirer.audio.broadcast.shoutcast.v2.ShoutcastV2Configuration;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.IAudioSegmentListener;
import io.github.dsheirer.audio.convert.InputAudioFormat;
import io.github.dsheirer.audio.convert.MP3AudioConverter;
import io.github.dsheirer.audio.convert.MP3Setting;
import io.github.dsheirer.gui.SDRTrunk;
import io.github.dsheirer.gui.playlist.radioreference.RadioReferenceDecoder;
import io.github.dsheirer.gui.playlist.radioreference.TalkgroupEncryption;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.configuration.ConfigurationLongIdentifier;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import io.github.dsheirer.alias.id.broadcast.BroadcastChannel;
import io.github.dsheirer.alias.id.ctcss.Ctcss;
import io.github.dsheirer.alias.id.priority.Priority;
import io.github.dsheirer.alias.id.talkgroup.Talkgroup;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.controller.channel.ChannelEvent;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.AuxDecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.mpt1327.DecodeConfigMPT1327;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.nbfm.DeemphasisMode;
import io.github.dsheirer.module.decode.nxdn.DecodeConfigNXDN;
import io.github.dsheirer.module.decode.nxdn.NXDNChannelMode;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.Encoding;
import io.github.dsheirer.module.decode.nxdn.layer3.type.TransmissionMode;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25;
import io.github.dsheirer.module.decode.squelch.SquelchDecoderConfig;
import io.github.dsheirer.module.decode.squelch.ctcss.CTCSSCode;
import io.github.dsheirer.module.decode.squelch.dcs.DCSCode;
import io.github.dsheirer.module.log.EventLogType;
import io.github.dsheirer.module.log.config.EventLogConfiguration;
import io.github.dsheirer.record.RecorderType;
import io.github.dsheirer.record.config.RecordConfiguration;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.monitor.HostSystemMetrics;
import io.github.dsheirer.protocol.Protocol;
import io.github.dsheirer.rrapi.type.TalkgroupCategory;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.manager.DiscoveredNetworkTuner;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.network.NetworkTuner;
import io.github.dsheirer.source.tuner.network.NetworkTunerConfiguration;
import io.github.dsheirer.source.tuner.network.NetworkTunerController;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerConfiguration;
import io.github.dsheirer.source.tuner.rtl.RTL2832TunerController;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xEmbeddedTuner;
import io.github.dsheirer.source.tuner.rtl.r8x.R8xTunerConfiguration;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.net.URLDecoder;
import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.prefs.Preferences;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Embedded, headless-safe HTTP API and initial browser dashboard. */
public class SdrTrunkWebServer implements IAudioSegmentListener
{
    private static final Logger mLog = LoggerFactory.getLogger(SdrTrunkWebServer.class);
    private static final Gson GSON = new Gson();
    public static final int DEFAULT_PORT = 10000;
    private static final long STARTED = System.currentTimeMillis();
    private static final String PASSWORD_KEY = "sdrtrunk.web.password";
    private static final String LEGACY_TOKEN_KEY = "sdrtrunk.web.access.token";
    private static final String BIND_KEY = "sdrtrunk.web.bind";
    private static final String DEFAULT_BIND_KEY = "sdrtrunk.web.defaultBind";
    private static final String PORT_KEY = "sdrtrunk.web.port";
    private static final String USERS_KEY = "sdrtrunk.web.users";
    private static final String DEFAULT_BIND_ADDRESS = "127.0.0.1";
    private static final String VCE_PREFERENCES_KEY = "sdrtrunk.web.vce.preferences";
    private static final String SESSION_COOKIE = "SDRTRUNK_SESSION";
    private static final String VCE_WEB_REVISION = "382-0.7.1-beta11";
    private static final long SESSION_LIFETIME_MS = 12L * 60L * 60L * 1000L;
    private static final int LOG_TAIL_BYTES = 1024 * 1024;
    private static final int LOG_ENTRY_LIMIT = 500;
    private static final int ACTIVITY_LIMIT = 5000;
    private static final long QUALITY_ACTIVE_WINDOW_MS = 10_000;
    private static final long DASHBOARD_BUCKET_MS = 60L * 60L * 1000L;
    private static final String ACTIVITY_FILE = "web_activity.json";
    private static final Pattern APPLICATION_LOG_LINE = Pattern.compile(
        "^(\\d{8} \\d{6}\\.\\d{3}) \\[([^]]+)]\\s+(\\w+)\\s+([^ ]+) - (.*)$");
    private static final DateTimeFormatter APPLICATION_LOG_TIME = DateTimeFormatter.ofPattern("yyyyMMdd HHmmss.SSS");
    private static volatile SdrTrunkWebServer ACTIVE;
    private final PlaylistManager mPlaylistManager;
    private final TunerManager mTunerManager;
    private final ResourceMonitor mResourceMonitor;
    private final UserPreferences mUserPreferences;
    private final WebTranscriptionService mTranscriptionService = new WebTranscriptionService();
    private volatile boolean mRadioReferenceInitialized;
    private volatile String mPassword;
    private final ConcurrentHashMap<String,WebSession> mSessions = new ConcurrentHashMap<>();
    private final SecureRandom mSecureRandom = new SecureRandom();
    private final Object mVcePreferenceLock = new Object();
    private JsonObject mVcePreferences;
    private long mVcePreferenceRevision = 1;
    private final WebSpectrumService mSpectrumService = new WebSpectrumService();
    private volatile Map<String,Object> mLatestAudioMetadata = Map.of();
    private final AtomicLong mLatestAudioSequence = new AtomicLong();
    private static final int LIVE_AUDIO_QUEUE_LIMIT = 200;
    private final ConcurrentLinkedDeque<LiveAudioCall> mLiveAudioQueue = new ConcurrentLinkedDeque<>();
    private final ConcurrentLinkedDeque<Map<String,Object>> mActivity = new ConcurrentLinkedDeque<>();
    private final AtomicBoolean mActivityWriteScheduled = new AtomicBoolean();
    private final Object mActivityFileLock = new Object();
    private final Path mActivityFile;
    private final Path mVcePreferenceFile;
    private HttpServer mServer;
    private ExecutorService mServerExecutor;
    private volatile String mBoundAddress;
    private volatile int mBoundPort;

    public record WebUser(String id, String username, String role, boolean enabled, long createdAt, long updatedAt) {}
    private record StoredWebUser(String id, String username, String passwordHash, String passwordSalt,
                                 String role, boolean enabled, long createdAt, long updatedAt) {}
    private record WebIdentity(String username, String role, boolean primary) {}
    private record WebSession(WebIdentity identity, long expiresAt) {}

    public SdrTrunkWebServer(PlaylistManager playlistManager, TunerManager tunerManager,
                             ResourceMonitor resourceMonitor, UserPreferences userPreferences, String password)
    {
        mPlaylistManager = playlistManager;
        mTunerManager = tunerManager;
        mResourceMonitor = resourceMonitor;
        mUserPreferences = userPreferences;
        mPassword = password;
        mActivityFile = userPreferences != null ?
            userPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve(ACTIVITY_FILE) : null;
        mVcePreferenceFile = userPreferences != null ?
            userPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("web_preferences.json") : null;
        mVcePreferences = loadVcePreferences();
        loadActivity();
        ACTIVE = this;
    }

    /** Updates the browser password and invalidates existing sessions. */
    public void setPassword(String password)
    {
        mPassword = password;
        mSessions.clear();
    }

    public static String getSavedPassword()
    {
        Preferences preferences = Preferences.userNodeForPackage(SDRTrunk.class);
        String password = preferences.get(PASSWORD_KEY, "");
        if(password.isBlank())
        {
            password = preferences.get(LEGACY_TOKEN_KEY, "");
            if(!password.isBlank())
            {
                preferences.put(PASSWORD_KEY, password);
                preferences.remove(LEGACY_TOKEN_KEY);
            }
        }
        return password;
    }

    public static void savePassword(String password)
    {
        Preferences preferences = Preferences.userNodeForPackage(SDRTrunk.class);
        preferences.put(PASSWORD_KEY, password);
        preferences.remove(LEGACY_TOKEN_KEY);
        if(ACTIVE != null) { ACTIVE.setPassword(password); }
    }

    /** Legacy API aliases retained for older integrations and saved settings. */
    public void setToken(String token) { setPassword(token); }
    public static String getSavedToken() { return getSavedPassword(); }
    public static void saveToken(String token) { savePassword(token); }

    public static String getSavedBindAddress()
    {
        return Preferences.userNodeForPackage(SDRTrunk.class).get(BIND_KEY,
            System.getProperty(DEFAULT_BIND_KEY, DEFAULT_BIND_ADDRESS));
    }

    public static int getSavedPort()
    {
        int port = Preferences.userNodeForPackage(SDRTrunk.class).getInt(PORT_KEY, DEFAULT_PORT);
        return port >= 1 && port <= 65535 ? port : DEFAULT_PORT;
    }

    public static String getEffectiveBindAddress()
    {
        return System.getProperty(BIND_KEY, getSavedBindAddress()).trim();
    }

    public static int getEffectivePort()
    {
        String override = System.getProperty(PORT_KEY);
        if(override != null)
        {
            try { return validPort(Integer.parseInt(override)); }
            catch(NumberFormatException e) { throw new IllegalArgumentException("Web interface port must be a number", e); }
        }
        return getSavedPort();
    }

    public static void saveNetworkSettings(String bindAddress, int port)
    {
        String bind = bindAddress == null ? "" : bindAddress.trim();
        validateNetworkSettings(bind, port);
        Preferences preferences = Preferences.userNodeForPackage(SDRTrunk.class);
        preferences.put(BIND_KEY, bind);
        preferences.putInt(PORT_KEY, port);
    }

    static void validateNetworkSettings(String bind, int port)
    {
        if(bind.isBlank()) { throw new IllegalArgumentException("Bind address cannot be empty"); }
        if(bind.contains("://") || bind.contains("/") || bind.chars().anyMatch(Character::isWhitespace))
        {
            throw new IllegalArgumentException("Enter an interface address or host name, not a URL");
        }
        validPort(port);
    }

    private static int validPort(int port)
    {
        if(port < 1 || port > 65535) { throw new IllegalArgumentException("Port must be between 1 and 65535"); }
        return port;
    }

    public static String getActualBindAddress()
    {
        return ACTIVE != null && ACTIVE.mServer != null ? ACTIVE.mBoundAddress : null;
    }

    public static int getActualPort()
    {
        return ACTIVE != null && ACTIVE.mServer != null ? ACTIVE.mBoundPort : 0;
    }

    public static synchronized List<WebUser> getWebUsers()
    {
        return loadStoredUsers().stream()
            .sorted(Comparator.comparing(StoredWebUser::username, String.CASE_INSENSITIVE_ORDER))
            .map(user -> new WebUser(user.id(), user.username(), user.role(), user.enabled(),
                user.createdAt(), user.updatedAt())).toList();
    }

    public static synchronized WebUser saveWebUser(String id, String username, String password,
                                                    String role, boolean enabled)
    {
        String normalizedUsername = username == null ? "" : username.trim();
        if(!normalizedUsername.matches("[A-Za-z0-9._-]{1,64}"))
        {
            throw new IllegalArgumentException("Username must be 1-64 letters, numbers, dots, dashes, or underscores");
        }
        if("admin".equalsIgnoreCase(normalizedUsername))
        {
            throw new IllegalArgumentException("The built-in admin username is reserved");
        }
        String normalizedRole = normalizeRole(role);
        List<StoredWebUser> users = new ArrayList<>(loadStoredUsers());
        StoredWebUser existing = users.stream().filter(user -> user.id().equals(id)).findFirst().orElse(null);
        if(users.stream().anyMatch(user -> !user.id().equals(id) &&
            user.username().equalsIgnoreCase(normalizedUsername)))
        {
            throw new IllegalArgumentException("A user with that username already exists");
        }
        String requestedPassword = password == null ? "" : password;
        if(existing == null && requestedPassword.isBlank())
        {
            throw new IllegalArgumentException("A password is required for a new user");
        }
        if(!requestedPassword.isBlank() && requestedPassword.length() < 8)
        {
            throw new IllegalArgumentException("Password must contain at least 8 characters");
        }

        long now = System.currentTimeMillis();
        String userId = existing == null ? UUID.randomUUID().toString() : existing.id();
        String salt = existing == null ? "" : existing.passwordSalt();
        String hash = existing == null ? "" : existing.passwordHash();
        if(!requestedPassword.isBlank())
        {
            byte[] saltBytes = new byte[16];
            new SecureRandom().nextBytes(saltBytes);
            salt = Base64.getEncoder().encodeToString(saltBytes);
            hash = hashPassword(requestedPassword, saltBytes);
        }
        StoredWebUser saved = new StoredWebUser(userId, normalizedUsername, hash, salt, normalizedRole,
            enabled, existing == null ? now : existing.createdAt(), now);
        users.removeIf(user -> user.id().equals(userId));
        users.add(saved);
        storeUsers(users);
        if(ACTIVE != null) { ACTIVE.invalidateUserSessions(existing != null ? existing.username() : normalizedUsername); }
        return new WebUser(saved.id(), saved.username(), saved.role(), saved.enabled(), saved.createdAt(), saved.updatedAt());
    }

    public static synchronized void deleteWebUser(String id)
    {
        List<StoredWebUser> users = new ArrayList<>(loadStoredUsers());
        StoredWebUser removed = users.stream().filter(user -> user.id().equals(id)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("User was not found"));
        users.removeIf(user -> user.id().equals(id));
        storeUsers(users);
        if(ACTIVE != null) { ACTIVE.invalidateUserSessions(removed.username()); }
    }

    private static String normalizeRole(String role)
    {
        return "admin".equalsIgnoreCase(role) ? "admin" : "operator";
    }

    private static synchronized List<StoredWebUser> loadStoredUsers()
    {
        String saved = Preferences.userNodeForPackage(SDRTrunk.class).get(USERS_KEY, "[]");
        List<StoredWebUser> users = new ArrayList<>();
        try
        {
            JsonArray array = GSON.fromJson(saved, JsonArray.class);
            if(array == null) { return users; }
            for(var element: array)
            {
                JsonObject user = element.getAsJsonObject();
                users.add(new StoredWebUser(stringValue(user, "id", ""), stringValue(user, "username", ""),
                    stringValue(user, "passwordHash", ""), stringValue(user, "passwordSalt", ""),
                    normalizeRole(stringValue(user, "role", "operator")), booleanValue(user, "enabled", true),
                    longValue(user, "createdAt", 0), longValue(user, "updatedAt", 0)));
            }
        }
        catch(Exception e)
        {
            mLog.error("Unable to read web user accounts", e);
        }
        return users.stream().filter(user -> !user.id().isBlank() && !user.username().isBlank()).toList();
    }

    private static synchronized void storeUsers(List<StoredWebUser> users)
    {
        JsonArray array = new JsonArray();
        for(StoredWebUser user: users)
        {
            JsonObject item = new JsonObject();
            item.addProperty("id", user.id());
            item.addProperty("username", user.username());
            item.addProperty("passwordHash", user.passwordHash());
            item.addProperty("passwordSalt", user.passwordSalt());
            item.addProperty("role", user.role());
            item.addProperty("enabled", user.enabled());
            item.addProperty("createdAt", user.createdAt());
            item.addProperty("updatedAt", user.updatedAt());
            array.add(item);
        }
        Preferences.userNodeForPackage(SDRTrunk.class).put(USERS_KEY, GSON.toJson(array));
    }

    static String hashPassword(String password, byte[] salt)
    {
        PBEKeySpec specification = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
        try
        {
            return Base64.getEncoder().encodeToString(
                SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(specification).getEncoded());
        }
        catch(Exception e)
        {
            throw new IllegalStateException("Unable to secure the web account password", e);
        }
        finally
        {
            specification.clearPassword();
        }
    }

    private static boolean passwordMatches(StoredWebUser user, String candidate)
    {
        return passwordHashMatches(user.passwordHash(), user.passwordSalt(), candidate);
    }

    static boolean passwordHashMatches(String passwordHash, String passwordSalt, String candidate)
    {
        if(candidate == null || passwordSalt == null || passwordSalt.isBlank() ||
            passwordHash == null || passwordHash.isBlank()) { return false; }
        try
        {
            byte[] salt = Base64.getDecoder().decode(passwordSalt);
            byte[] expected = Base64.getDecoder().decode(passwordHash);
            byte[] actual = Base64.getDecoder().decode(hashPassword(candidate, salt));
            return MessageDigest.isEqual(expected, actual);
        }
        catch(IllegalArgumentException e)
        {
            return false;
        }
    }

    private void invalidateUserSessions(String username)
    {
        mSessions.entrySet().removeIf(entry -> entry.getValue().identity().username().equalsIgnoreCase(username));
    }

    public static boolean isRunning()
    {
        return ACTIVE != null && ACTIVE.mServer != null;
    }

    public static synchronized void restartActive() throws IOException
    {
        if(ACTIVE == null) { throw new IOException("Web interface has not been initialized"); }
        ACTIVE.stop();
        ACTIVE.start();
    }

    public void start() throws IOException
    {
        String bind = getEffectiveBindAddress();
        int port = getEffectivePort();
        mServer = HttpServer.create(new InetSocketAddress(bind, port), 0);
        mBoundAddress = mServer.getAddress().getAddress() != null ?
            mServer.getAddress().getAddress().getHostAddress() : bind;
        mBoundPort = mServer.getAddress().getPort();
        mServerExecutor = Executors.newVirtualThreadPerTaskExecutor();
        mServer.setExecutor(mServerExecutor);
        mServer.createContext("/api/v1/health", exchange -> json(exchange, 200, Map.of("status", "ok")));
        mServer.createContext("/api/v1/login", this::login);
        mServer.createContext("/api/v1/logout", authenticated(this::logout));
        mServer.createContext("/api/v1/auth/session", this::vceSession);
        mServer.createContext("/api/v1/auth/login", this::vceLogin);
        mServer.createContext("/api/v1/auth/logout", this::vceLogout);
        mServer.createContext("/api/v1/me/preferences", authenticated(this::vcePreferences));
        mServer.createContext("/api/v1/status", authenticated(this::status));
        mServer.createContext("/api/v1/quality", authenticated(this::vceQuality));
        mServer.createContext("/api/v1/dashboard", authenticated(this::vceDashboard));
        mServer.createContext("/api/v1/activity/actions", authenticated(this::vceActivityActions));
        mServer.createContext("/api/v1/activity/radios", authenticated(this::vceActivityRadios));
        mServer.createContext("/api/v1/channels", authenticatedWithAdminMutations(this::channels));
        mServer.createContext("/api/v1/channel-options", administrator(this::channelOptions));
        mServer.createContext("/api/v1/talkgroups", authenticatedWithAdminMutations(this::talkgroups));
        mServer.createContext("/api/v1/tuners", authenticatedWithAdminMutations(this::tuners));
        mServer.createContext("/api/v1/spectrum", authenticatedWithAdminMutations(this::spectrum));
        mServer.createContext("/api/v1/broadcasters", administrator(this::broadcasters));
        mServer.createContext("/api/v1/remote-destinations", administrator(this::remoteDestinations));
        mServer.createContext("/api/v1/channel-control", administrator(this::channelControl));
        mServer.createContext("/api/v1/recordings", authenticated(this::recordings));
        mServer.createContext("/api/v1/recording-audio", authenticated(this::recordingAudio));
        mServer.createContext("/api/v1/live-audio", authenticated(this::liveAudio));
        mServer.createContext("/api/v1/live-status", authenticated(this::liveStatus));
        mServer.createContext("/api/v1/activity", authenticated(this::activity));
        mServer.createContext("/api/v1/transcripts", authenticated(this::transcripts));
        mServer.createContext("/api/v1/whisper-settings", administrator(this::whisperSettings));
        mServer.createContext("/api/v1/web-settings", administrator(this::webSettings));
        mServer.createContext("/api/v1/users", administrator(this::webUsers));
        mServer.createContext("/api/v1/application-log", administrator(this::applicationLog));
        mServer.createContext("/api/v1/event-logs", administrator(this::eventLogs));
        mServer.createContext("/api/v1/radioreference", administrator(this::radioReference));
        mServer.createContext("/", this::dashboard);
        mServer.start();
        mLog.info("sdrtrunk web interface listening at http://{}:{}", mBoundAddress, mBoundPort);
        if(mPassword == null || mPassword.isBlank())
        {
            mLog.warn("Web interface password is empty; API requests will be denied");
        }
    }

    public void stop()
    {
        persistActivity();
        if(mServer != null)
        {
            mServer.stop(2);
            mServer = null;
            if(mServerExecutor != null)
            {
                mServerExecutor.shutdownNow();
                mServerExecutor = null;
            }
            mBoundAddress = null;
            mBoundPort = 0;
        }
    }

    private HttpHandler authenticated(HttpHandler handler)
    {
        return exchange ->
        {
            try
            {
                if(requestIdentity(exchange) == null)
                {
                    json(exchange, 401, Map.of("error", "authentication required"));
                    return;
                }
                handler.handle(exchange);
            }
            catch(Exception e)
            {
                mLog.error("Web API request failed: {} {}", exchange.getRequestMethod(),
                    exchange.getRequestURI(), e);
                try
                {
                    json(exchange, 500, Map.of("error", e.getMessage() != null ? e.getMessage() :
                        e.getClass().getSimpleName()));
                }
                catch(Exception responseError)
                {
                    exchange.close();
                }
            }
        };
    }

    private HttpHandler administrator(HttpHandler handler)
    {
        return exchange ->
        {
            try
            {
                WebIdentity identity = requestIdentity(exchange);
                if(identity == null)
                {
                    json(exchange, 401, Map.of("error", "authentication required"));
                    return;
                }
                if(!"admin".equals(identity.role()))
                {
                    json(exchange, 403, Map.of("error", "administrator access required"));
                    return;
                }
                handler.handle(exchange);
            }
            catch(Exception e)
            {
                mLog.error("Web administration request failed: {} {}", exchange.getRequestMethod(),
                    exchange.getRequestURI(), e);
                try { json(exchange, 500, Map.of("error", errorMessage(e))); }
                catch(Exception responseError) { exchange.close(); }
            }
        };
    }

    private HttpHandler authenticatedWithAdminMutations(HttpHandler handler)
    {
        return exchange ->
        {
            try
            {
                WebIdentity identity = requestIdentity(exchange);
                if(identity == null)
                {
                    json(exchange, 401, Map.of("error", "authentication required"));
                    return;
                }
                boolean readOnly = "GET".equalsIgnoreCase(exchange.getRequestMethod()) ||
                    "HEAD".equalsIgnoreCase(exchange.getRequestMethod());
                if(!readOnly && !"admin".equals(identity.role()))
                {
                    json(exchange, 403, Map.of("error", "administrator access required"));
                    return;
                }
                handler.handle(exchange);
            }
            catch(Exception e)
            {
                mLog.error("Web API request failed: {} {}", exchange.getRequestMethod(), exchange.getRequestURI(), e);
                try { json(exchange, 500, Map.of("error", errorMessage(e))); }
                catch(Exception responseError) { exchange.close(); }
            }
        };
    }

    private void login(HttpExchange exchange) throws IOException
    {
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        String username = request != null && request.has("username") ? request.get("username").getAsString() : "";
        String password = request != null && request.has("password") ? request.get("password").getAsString() : "";
        WebIdentity identity = authenticate(username, password);
        if(identity == null)
        {
            json(exchange, 401, Map.of("error", "incorrect username or password"));
            return;
        }
        setSessionCookie(exchange, createSession(identity));
        json(exchange, 200, Map.of("authenticated", true, "username", identity.username(),
            "role", identity.role()));
    }

    private void logout(HttpExchange exchange) throws IOException
    {
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        String session = cookie(exchange, SESSION_COOKIE);
        if(session != null) { mSessions.remove(session); }
        exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE +
            "=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
        json(exchange, 200, Map.of("authenticated", false));
    }

    /** Compatibility session contract used by the VCE web client. */
    private void vceSession(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        vceJson(exchange, 200, vceSessionResponse(requestIdentity(exchange)));
    }

    private void vceLogin(HttpExchange exchange) throws IOException
    {
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        String username = request != null && request.has("username") ? request.get("username").getAsString() : "";
        String password = request != null && request.has("password") ? request.get("password").getAsString() : "";
        WebIdentity identity = authenticate(username, password);
        if(identity == null)
        {
            vceError(exchange, 401, "invalid_credentials", "The username or password is invalid");
            return;
        }
        setSessionCookie(exchange, createSession(identity));
        vceJson(exchange, 200, vceSessionResponse(identity));
    }

    private void vceLogout(HttpExchange exchange) throws IOException
    {
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        String session = cookie(exchange, SESSION_COOKIE);
        if(session != null) { mSessions.remove(session); }
        exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE +
            "=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0");
        vceJson(exchange, 200, vceSessionResponse(null));
    }

    private Map<String,Object> vceSessionResponse(WebIdentity identity)
    {
        boolean authenticated = identity != null;
        boolean administrator = authenticated && "admin".equals(identity.role());
        Map<String,Boolean> capabilities = new LinkedHashMap<>();
        capabilities.put("web-access", authenticated);
        capabilities.put("dashboard", authenticated);
        capabilities.put("live", false);
        capabilities.put("tuner-spectrum", authenticated);
        capabilities.put("admin-tuners", administrator);
        capabilities.put("radio", false);
        capabilities.put("credits", authenticated);
        capabilities.put("csv-export", false);
        capabilities.put("call-audio", false);
        capabilities.put("recordings", authenticated);
        capabilities.put("admin-recordings", false);
        capabilities.put("receiver-health", false);
        capabilities.put("admin-aliases", administrator);
        capabilities.put("admin-channels", administrator);
        capabilities.put("admin-streaming", administrator);
        capabilities.put("admin-settings", administrator);
        capabilities.put("admin-radioreference", administrator);
        capabilities.put("admin-users", administrator);
        capabilities.put("admin-access", administrator);
        capabilities.put("user-settings", authenticated);

        Map<String,Object> response = new LinkedHashMap<>();
        response.put("configured", mPassword != null && !mPassword.isBlank());
        response.put("authenticated", authenticated);
        response.put("tier", authenticated ? (administrator ? "admin" : "user") : "public");
        response.put("capabilities", capabilities);
        if(authenticated)
        {
            response.put("username", identity.username());
            response.put("primary", identity.primary());
            response.put("guide_dismissal_scope", "sdrtrunk-0.7.1-beta11-" + identity.username());
            response.put("csrf_token", "sdrtrunk-session");
        }
        return response;
    }

    private void vcePreferences(HttpExchange exchange) throws IOException
    {
        synchronized(mVcePreferenceLock)
        {
            if("GET".equals(exchange.getRequestMethod()))
            {
                json(exchange, 200, Map.of("revision", mVcePreferenceRevision,
                    "preferences", mVcePreferences));
                return;
            }
            if("PUT".equals(exchange.getRequestMethod()))
            {
                String ifMatch = exchange.getRequestHeaders().getFirst("If-Match");
                if(ifMatch != null && !ifMatch.replace("\"", "").equals(String.valueOf(mVcePreferenceRevision)))
                {
                    vceError(exchange, 409, "preference_conflict", "The preference profile changed");
                    return;
                }
                JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8), JsonObject.class);
                if(request == null || !request.has("version") || request.get("version").getAsInt() != 12)
                {
                    vceError(exchange, 400, "invalid_preferences", "The preference profile is invalid");
                    return;
                }
                mVcePreferences = mergeVcePreferences(defaultVcePreferences(), request, "");
                mVcePreferenceRevision++;
                persistVcePreferences();
                json(exchange, 200, Map.of("revision", mVcePreferenceRevision,
                    "preferences", mVcePreferences));
                return;
            }
        }
        methodNotAllowed(exchange);
    }

    private static JsonObject defaultVcePreferences()
    {
        return GSON.fromJson(VCE_DEFAULT_PREFERENCES, JsonObject.class);
    }

    private JsonObject loadVcePreferences()
    {
        JsonObject defaults = defaultVcePreferences();
        String saved = "";
        if(mVcePreferenceFile != null && Files.isRegularFile(mVcePreferenceFile))
        {
            try { saved = Files.readString(mVcePreferenceFile, StandardCharsets.UTF_8); }
            catch(IOException e) { mLog.warn("Unable to read saved VCE web preferences; using defaults", e); }
        }
        if(saved.isBlank())
        {
            saved = Preferences.userNodeForPackage(SDRTrunk.class).get(VCE_PREFERENCES_KEY, "");
        }
        if(!saved.isBlank())
        {
            try
            {
                JsonObject parsed = GSON.fromJson(saved, JsonObject.class);
                if(parsed != null && parsed.has("version") && parsed.get("version").getAsInt() == 12)
                {
                    return mergeVcePreferences(defaults, parsed, "");
                }
                mLog.info("Resetting an older VCE web preference profile to the current schema");
            }
            catch(Exception e)
            {
                mLog.warn("Unable to load saved VCE web preferences; using defaults", e);
            }
        }
        return defaults;
    }

    private void persistVcePreferences()
    {
        String json = GSON.toJson(mVcePreferences);
        if(mVcePreferenceFile != null)
        {
            Path temporary = mVcePreferenceFile.resolveSibling(mVcePreferenceFile.getFileName() + ".tmp");
            try
            {
                Files.createDirectories(mVcePreferenceFile.getParent());
                Files.writeString(temporary, json, StandardCharsets.UTF_8);
                try
                {
                    Files.move(temporary, mVcePreferenceFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                }
                catch(AtomicMoveNotSupportedException e)
                {
                    Files.move(temporary, mVcePreferenceFile, StandardCopyOption.REPLACE_EXISTING);
                }
                Preferences.userNodeForPackage(SDRTrunk.class).remove(VCE_PREFERENCES_KEY);
                return;
            }
            catch(IOException e)
            {
                mLog.warn("Unable to save VCE web preferences [{}]", mVcePreferenceFile, e);
            }
        }
        try { Preferences.userNodeForPackage(SDRTrunk.class).put(VCE_PREFERENCES_KEY, json); }
        catch(IllegalArgumentException e)
        {
            mLog.warn("VCE web preferences are too large to persist; retaining them for this session");
        }
    }

    /** Fills newly introduced settings and removes stale keys while preserving compatible user choices. */
    private static JsonObject mergeVcePreferences(JsonObject defaults, JsonObject saved, String path)
    {
        JsonObject merged = defaults.deepCopy();
        for(String key: defaults.keySet())
        {
            if(!saved.has(key) || saved.get(key).isJsonNull()) { continue; }
            var expected = defaults.get(key);
            var actual = saved.get(key);
            String childPath = path.isBlank() ? key : path + "." + key;
            if("tables".equals(childPath) && actual.isJsonObject())
            {
                merged.add(key, normalizeVceTablePreferences(actual.getAsJsonObject()));
            }
            else if(expected.isJsonObject() && actual.isJsonObject())
            {
                merged.add(key, mergeVcePreferences(expected.getAsJsonObject(), actual.getAsJsonObject(), childPath));
            }
            else if(expected.isJsonArray() == actual.isJsonArray() && expected.isJsonPrimitive() == actual.isJsonPrimitive())
            {
                merged.add(key, actual.deepCopy());
            }
        }
        return merged;
    }

    private static JsonObject normalizeVceTablePreferences(JsonObject savedTables)
    {
        JsonObject normalized = new JsonObject();
        for(String tableId: savedTables.keySet())
        {
            if(!savedTables.get(tableId).isJsonObject()) { continue; }
            JsonObject saved = savedTables.getAsJsonObject(tableId);
            if(!saved.has("schema") || !saved.get("schema").isJsonArray() ||
                !saved.has("column_order") || !saved.get("column_order").isJsonArray() ||
                !saved.has("column_widths") || !saved.get("column_widths").isJsonObject() ||
                !saved.has("hidden_columns") || !saved.get("hidden_columns").isJsonArray())
            {
                continue;
            }
            JsonObject table = new JsonObject();
            table.add("schema", saved.get("schema").deepCopy());
            table.add("column_order", saved.get("column_order").deepCopy());
            table.add("column_widths", saved.get("column_widths").deepCopy());
            table.add("hidden_columns", saved.get("hidden_columns").deepCopy());
            table.add("collapsed_groups", saved.has("collapsed_groups") && saved.get("collapsed_groups").isJsonArray() ?
                saved.get("collapsed_groups").deepCopy() : new JsonArray());
            normalized.add(tableId, table);
        }
        return normalized;
    }

    private boolean passwordMatches(String candidate)
    {
        return mPassword != null && !mPassword.isBlank() && candidate != null &&
            MessageDigest.isEqual(mPassword.getBytes(StandardCharsets.UTF_8),
                candidate.getBytes(StandardCharsets.UTF_8));
    }

    private WebIdentity requestIdentity(HttpExchange exchange)
    {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if(authorization != null && authorization.startsWith("Bearer ") &&
            passwordMatches(authorization.substring(7)))
        {
            return new WebIdentity("admin", "admin", true);
        }
        return sessionIdentity(exchange);
    }

    private WebIdentity sessionIdentity(HttpExchange exchange)
    {
        String token = cookie(exchange, SESSION_COOKIE);
        if(token == null) { return null; }
        WebSession session = mSessions.get(token);
        if(session == null || session.expiresAt() < System.currentTimeMillis())
        {
            mSessions.remove(token);
            return null;
        }
        return session.identity();
    }

    private WebIdentity authenticate(String username, String password)
    {
        String normalized = username == null ? "" : username.trim();
        if((normalized.isBlank() || "admin".equalsIgnoreCase(normalized)) && passwordMatches(password))
        {
            return new WebIdentity("admin", "admin", true);
        }
        for(StoredWebUser user: loadStoredUsers())
        {
            if(user.enabled() && user.username().equalsIgnoreCase(normalized) && passwordMatches(user, password))
            {
                return new WebIdentity(user.username(), user.role(), false);
            }
        }
        return null;
    }

    private String createSession(WebIdentity identity)
    {
        byte[] random = new byte[32];
        mSecureRandom.nextBytes(random);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(random);
        mSessions.put(token, new WebSession(identity, System.currentTimeMillis() + SESSION_LIFETIME_MS));
        return token;
    }

    private static void setSessionCookie(HttpExchange exchange, String token)
    {
        exchange.getResponseHeaders().add("Set-Cookie", SESSION_COOKIE + "=" + token +
            "; HttpOnly; SameSite=Strict; Path=/; Max-Age=" + (SESSION_LIFETIME_MS / 1000));
    }

    private static String cookie(HttpExchange exchange, String name)
    {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if(cookies == null) { return null; }
        for(String part: cookies.split(";"))
        {
            String[] pair = part.trim().split("=", 2);
            if(pair.length == 2 && pair[0].equals(name)) { return pair[1]; }
        }
        return null;
    }

    private void status(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("started", Instant.ofEpochMilli(STARTED).toString());
        result.put("uptimeMs", System.currentTimeMillis() - STARTED);
        HostSystemMetrics.Snapshot metrics = HostSystemMetrics.snapshot();
        result.put("cpu", metrics.cpuLoad());
        result.put("cpuAvailable", metrics.hasCpuLoad());
        result.put("memoryUsed", metrics.usedMemory());
        result.put("memoryAllocated", metrics.totalMemory());
        result.put("memoryMaximum", metrics.totalMemory());
        result.put("memoryAvailable", metrics.hasHostMemory());
        result.put("jvmMemoryUsed", metrics.jvmUsedMemory());
        result.put("jvmMemoryMaximum", metrics.jvmMaximumMemory());
        result.put("processors", metrics.processors());
        result.put("operatingSystem", metrics.operatingSystem());
        result.put("recordingsSize", mResourceMonitor.fileSizeRecordingsProperty().get());
        result.put("eventLogsSize", mResourceMonitor.fileSizeEventLogsProperty().get());
        result.put("stats_logging", Map.of("summary_configured", true, "detailed_history_configured", false,
            "summary_active", true, "detailed_history_active", false, "state", "RUNNING"));
        result.put("database", Map.of("stats_logging_enabled", true, "detailed_history_enabled", false,
            "detailed_history_available", false));
        json(exchange, 200, result);
    }

    /** Supplies the current channel tiles used by the unmodified VCE Main workspace. */
    private void vceQuality(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        long now = System.currentTimeMillis();
        List<Map<String,Object>> rows = new ArrayList<>();
        List<Channel> channels = mPlaylistManager != null ?
            new ArrayList<>(mPlaylistManager.getChannelModel().getChannels()) : List.of();
        for(Channel channel: channels)
        {
            if(!channel.isProcessing()) { continue; }
            Map<String,Object> row = new LinkedHashMap<>();
            row.put("configuration_id", UUID.nameUUIDFromBytes(("sdrtrunk-channel:" + channel.getChannelID())
                .getBytes(StandardCharsets.UTF_8)).toString());
            row.put("channel_name", valueOrEmpty(channel.getName()));
            row.put("name", valueOrEmpty(channel.getName()));
            row.put("radio_system_name", valueOrEmpty(channel.getSystem()));
            row.put("site_name", valueOrEmpty(channel.getSite()));
            row.put("protocol", channel.getDecodeConfiguration() != null ?
                channel.getDecodeConfiguration().getDecoderType().name() : "");
            row.put("channel_kind", channelKind(channel));
            row.put("processing", true);
            row.put("last_observed_ms", now);
            if(channel.getSourceConfiguration() instanceof SourceConfigTuner source)
            {
                row.put("frequency_hz", source.getFrequency());
                row.put("quality_frequency_hz", source.getFrequency());
            }
            Map<String,Object> activity = latestChannelActivity(channel);
            long activityTime = activityEndTime(activity);
            boolean active = !activity.isEmpty() && now - activityTime <= QUALITY_ACTIVE_WINDOW_MS;
            row.put("receiver_state", active ? "ACTIVE" : "IDLE");
            row.put("decode_status", active ? "ACTIVE" : "WAITING_FOR_SYNC");
            row.put("rf_signal_available", false);
            row.put("decode_health_available", false);
            if(!activity.isEmpty())
            {
                row.put("last_activity_ms", activityTime);
                if(active)
                {
                    row.put("active_alias", activity.getOrDefault("alias", ""));
                    row.put("active_talkgroup", activity.getOrDefault("talkgroup", ""));
                    row.put("active_source", activity.getOrDefault("source", ""));
                    long activityFrequency = numberValue(activity.get("frequency"));
                    if(activityFrequency > 0) { row.put("quality_frequency_hz", activityFrequency); }
                }
            }
            rows.add(row);
        }
        int limit = (int)Math.max(1, Math.min(500, longQueryValue(exchange, "limit", 100)));
        int offset = (int)Math.max(0, Math.min(rows.size(), longQueryValue(exchange, "offset", 0)));
        int end = Math.min(rows.size(), offset + limit);
        boolean hasMore = end < rows.size();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("rows", rows.subList(offset, end));
        response.put("limit", limit);
        response.put("offset", offset);
        response.put("total_count", rows.size());
        response.put("has_more", hasMore);
        response.put("next_offset", hasMore ? end : null);
        response.put("range", "current");
        vceJson(exchange, 200, response);
    }

    /** Retained call activity and restart-safe streaming totals for VCE's Main and Calls workspaces. */
    private void vceDashboard(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        long now = System.currentTimeMillis();
        Object lastActivityTime = mActivity.isEmpty() ? null : mActivity.peekFirst().get("time");
        long lastSeen = lastActivityTime instanceof Number number ? number.longValue() : 0;
        long cutoff = now - TimeUnit.HOURS.toMillis(24);
        long from = cutoff - Math.floorMod(cutoff, DASHBOARD_BUCKET_MS);
        long to = now + (DASHBOARD_BUCKET_MS - Math.floorMod(now, DASHBOARD_BUCKET_MS));
        List<Map<String,Object>> recent = new ArrayList<>();
        Set<String> callIds = new HashSet<>();
        int recorded = 0;
        Map<String,Map<String,Object>> series = new HashMap<>();
        Map<String,Map<String,Object>> sourceActivity = new HashMap<>();
        Map<String,Map<String,Object>> destinations = new HashMap<>();
        Map<String,Map<String,Object>> sources = new HashMap<>();
        for(Map<String,Object> item: mActivity)
        {
            long time = numberValue(item.get("time"));
            if(time < cutoff || !isCallActivity(item))
            {
                continue;
            }
            String id = String.valueOf(item.getOrDefault("id", activityId(item)));
            if(!callIds.add(id)) { continue; }
            boolean wasRecorded = Boolean.TRUE.equals(item.getOrDefault("recorded", true));
            if(wasRecorded) { recorded++; }
            if(recent.size() < 25) { recent.add(item); }

            String protocol = activityProtocol(item);
            String kind = activityChannelKind(item);
            long bucket = time - Math.floorMod(time, DASHBOARD_BUCKET_MS);
            Map<String,Object> seriesRow = series.computeIfAbsent(bucket + "\u0000" + protocol + "\u0000" + kind,
                ignored -> dashboardCountRow(protocol, kind));
            seriesRow.put("time_ms", bucket);
            increment(seriesRow, "logical_call_count");
            if(wasRecorded) { increment(seriesRow, "recorded_logical_call_count"); }

            String system = activitySystem(item);
            String channel = String.valueOf(item.getOrDefault("channel", ""));
            long frequency = numberValue(item.get("frequency"));
            if("CONVENTIONAL".equals(kind))
            {
                String key = protocol + "\u0000" + system + "\u0000" + channel + "\u0000" + frequency;
                Map<String,Object> sourceRow = sourceActivity.computeIfAbsent(key, ignored ->
                    dashboardCountRow(protocol, kind));
                sourceRow.put("name", !channel.isBlank() ? channel : "Configured receiver");
                sourceRow.put("source_label", !channel.isBlank() ? channel : "Configured receiver");
                sourceRow.put("radio_system_name", system);
                if(frequency > 0) { sourceRow.put("frequency_hz", frequency); }
                increment(sourceRow, "logical_call_count");
                if(wasRecorded) { increment(sourceRow, "recorded_logical_call_count"); }
            }

            String destinationId = activityIdentity(item, "talkgroupId", "talkgroup");
            if(!destinationId.isBlank())
            {
                String identityKind = String.valueOf(item.getOrDefault("destinationKind", "talkgroup"));
                String key = protocol + "\u0000" + kind + "\u0000" + system + "\u0000" + channel +
                    "\u0000" + identityKind + "\u0000" + destinationId;
                Map<String,Object> destination = destinations.computeIfAbsent(key, ignored ->
                    dashboardIdentityRow(item, protocol, kind, identityKind, destinationId));
                increment(destination, "logical_call_count");
                if(wasRecorded) { increment(destination, "recorded_logical_call_count"); }
            }

            String sourceId = activityIdentity(item, "sourceId", "source");
            if(!sourceId.isBlank() && !"UNKNOWN".equalsIgnoreCase(sourceId))
            {
                String key = protocol + "\u0000" + kind + "\u0000" + system + "\u0000" + channel +
                    "\u0000" + sourceId;
                Map<String,Object> source = sources.computeIfAbsent(key, ignored ->
                    dashboardIdentityRow(item, protocol, kind, "radio", sourceId));
                source.put("alias_name", item.getOrDefault("sourceAlias", ""));
                increment(source, "logical_call_count");
                if(wasRecorded) { increment(source, "recorded_logical_call_count"); }
            }
        }
        Map<String,Object> streaming = streamingDashboardSummary();
        Map<String,Object> callActivity = new LinkedHashMap<>();
        callActivity.put("totals", Map.of("logical_call_count", callIds.size(),
            "recorded_logical_call_count", recorded, "stream_submitted_logical_call_count", 0));
        //The broadcaster counters are retained lifetime totals, not a per-call last-24-hour association.  They are
        //reported accurately in streaming_summary; mark this time-windowed metric unavailable instead of mixing scopes.
        callActivity.put("metric_coverage", Map.of("stream_submitted_logical_call_count", "NOT_COLLECTED"));
        callActivity.put("from_ms", from);
        callActivity.put("to_ms", to);
        callActivity.put("bucket_ms", DASHBOARD_BUCKET_MS);
        callActivity.put("series", series.values().stream()
            .sorted(Comparator.comparingLong(row -> numberValue(row.get("time_ms")))).toList());
        callActivity.put("coverage", series.values().stream()
            .map(row -> Map.<String,Object>of("protocol", row.get("protocol"),
                "channel_kind", row.get("channel_kind"), "status", "COLLECTED",
                "logical_call_count", "COLLECTED", "recorded_logical_call_count", "COLLECTED",
                "stream_submitted_logical_call_count", "NOT_COLLECTED"))
            .distinct().toList());
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("last_seen_ms", lastSeen);
        response.put("call_activity", callActivity);
        response.put("recent_calls", recent);
        response.put("streaming_summary", streaming);
        response.put("source_activity_24h", rankedRows(sourceActivity));
        response.put("top_destinations", rankedRows(destinations));
        response.put("top_sources", rankedRows(sources));
        vceJson(exchange, 200, response);
    }

    private Map<String,Object> streamingDashboardSummary()
    {
        List<Map<String,Object>> rows = new ArrayList<>();
        int uploaded = 0;
        int queued = 0;
        int errors = 0;
        if(mPlaylistManager != null)
        {
            for(BroadcastConfiguration configuration:
                mPlaylistManager.getBroadcastModel().getBroadcastConfigurations())
            {
                Map<String,Object> row;
                if(configuration instanceof RemoteApiConfiguration remote)
                {
                    row = remoteDestinationMap(remote);
                    row.put("queue", row.getOrDefault("queuedCalls", 0));
                }
                else
                {
                    ConfiguredBroadcast configured = findConfiguredBroadcast(configuration);
                    if(configured == null) { continue; }
                    row = broadcasterMap(configured);
                }
                int rowUploaded = row.get("sent") instanceof Number number ? number.intValue() : 0;
                int rowQueued = row.get("queue") instanceof Number number ? number.intValue() : 0;
                int rowErrors = row.get("errors") instanceof Number number ? number.intValue() : 0;
                uploaded += rowUploaded;
                queued += rowQueued;
                errors += rowErrors;
                rows.add(row);
            }
        }
        return Map.of("uploaded", uploaded, "queued", queued, "errors", errors, "destinations", rows);
    }

    private void vceActivityActions(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        long cutoff = System.currentTimeMillis() - activityRangeMillis(queryValue(exchange, "range"));
        Map<String,Integer> counts = new HashMap<>();
        for(Map<String,Object> item: mActivity)
        {
            if(numberValue(item.get("time")) < cutoff) { continue; }
            counts.merge(activityAction(item), 1, Integer::sum);
        }
        List<Map<String,Object>> rows = counts.entrySet().stream()
            .sorted(Map.Entry.<String,Integer>comparingByValue().reversed())
            .map(entry -> Map.<String,Object>of("action", entry.getKey(),
                "observation_count", entry.getValue())).toList();
        vceJson(exchange, 200, Map.of("rows", rows,
            "total_count", counts.values().stream().mapToInt(Integer::intValue).sum()));
    }

    private void vceActivityRadios(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        int limit = (int)Math.max(1, Math.min(100, longQueryValue(exchange, "limit", 100)));
        int offset = (int)Math.max(0, longQueryValue(exchange, "offset", 0));
        long cutoff = System.currentTimeMillis() - activityRangeMillis(queryValue(exchange, "range"));
        String selectedAction = valueOrEmpty(queryValue(exchange, "action")).toUpperCase();
        Map<String,Map<String,Object>> radios = new HashMap<>();
        int retained = 0;
        int identified = 0;
        int unknown = 0;
        for(Map<String,Object> item: mActivity)
        {
            if(numberValue(item.get("time")) < cutoff ||
                (!selectedAction.isBlank() && !selectedAction.equals(activityAction(item))))
            {
                continue;
            }
            retained++;
            String sourceId = activityIdentity(item, "sourceId", "source");
            if(sourceId.isBlank() || "UNKNOWN".equalsIgnoreCase(sourceId))
            {
                unknown++;
                continue;
            }
            identified++;
            String system = activitySystem(item);
            String channel = String.valueOf(item.getOrDefault("channel", ""));
            String key = system + "\u0000" + channel + "\u0000" + sourceId;
            Map<String,Object> row = radios.computeIfAbsent(key, ignored ->
            {
                Map<String,Object> created = new LinkedHashMap<>();
                created.put("radio_id", sourceId);
                created.put("native_id", sourceId);
                created.put("alias_name", item.getOrDefault("sourceAlias", ""));
                created.put("radio_system_name", system);
                created.put("name", !system.isBlank() ? system : channel);
                created.put("source_label", channel);
                created.put("protocol", activityProtocol(item));
                created.put("channel_kind", activityChannelKind(item));
                created.put("observation_count", 0);
                created.put("last_seen_ms", 0L);
                return created;
            });
            increment(row, "observation_count");
            row.put("last_seen_ms", Math.max(numberValue(row.get("last_seen_ms")), activityEndTime(item)));
        }
        List<Map<String,Object>> allRows = radios.values().stream()
            .sorted(Comparator.<Map<String,Object>>comparingLong(row -> numberValue(row.get("observation_count")))
                .reversed().thenComparing(Comparator.comparingLong(
                    (Map<String,Object> row) -> numberValue(row.get("last_seen_ms"))).reversed()))
            .toList();
        int boundedOffset = Math.min(offset, allRows.size());
        int end = Math.min(allRows.size(), boundedOffset + limit);
        boolean hasMore = end < allRows.size();
        Map<String,Object> response = new LinkedHashMap<>();
        response.put("rows", allRows.subList(boundedOffset, end));
        response.put("total_count", allRows.size());
        response.put("limit", limit);
        response.put("offset", boundedOffset);
        response.put("has_more", hasMore);
        response.put("next_offset", hasMore ? end : null);
        response.put("action_observation_count", retained);
        response.put("retained_observation_count", retained);
        response.put("identified_observation_count", identified);
        response.put("unknown_source_observation_count", unknown);
        vceJson(exchange, 200, response);
    }

    private static Map<String,Object> dashboardCountRow(String protocol, String channelKind)
    {
        Map<String,Object> row = new LinkedHashMap<>();
        row.put("protocol", protocol);
        row.put("channel_kind", channelKind);
        row.put("logical_call_count", 0);
        row.put("recorded_logical_call_count", 0);
        row.put("stream_submitted_logical_call_count", null);
        return row;
    }

    private static Map<String,Object> dashboardIdentityRow(Map<String,Object> item, String protocol,
                                                            String channelKind, String identityKind,
                                                            String identityId)
    {
        Map<String,Object> row = dashboardCountRow(protocol, channelKind);
        row.put("identity_kind", identityKind);
        row.put("identity_id", identityId);
        row.put("native_id", identityId);
        row.put("alias_name", item.getOrDefault("alias", ""));
        row.put("radio_system_name", activitySystem(item));
        row.put("name", activitySystem(item));
        row.put("source_label", item.getOrDefault("channel", ""));
        return row;
    }

    private static List<Map<String,Object>> rankedRows(Map<String,Map<String,Object>> values)
    {
        return values.values().stream()
            .sorted(Comparator.<Map<String,Object>>comparingLong(row -> numberValue(row.get("logical_call_count")))
                .reversed())
            .limit(50).toList();
    }

    private static void increment(Map<String,Object> row, String field)
    {
        row.put(field, numberValue(row.get(field)) + 1);
    }

    private static long numberValue(Object value)
    {
        if(value instanceof Number number) { return number.longValue(); }
        try { return Long.parseLong(String.valueOf(value)); }
        catch(Exception e) { return 0; }
    }

    private static long activityEndTime(Map<String,Object> item)
    {
        long end = numberValue(item.get("end"));
        if(end > 0) { return end; }
        return numberValue(item.get("time")) + Math.max(0, numberValue(item.get("duration")));
    }

    private static boolean isCallActivity(Map<String,Object> item)
    {
        return String.valueOf(item.getOrDefault("type", "")).toUpperCase().contains("CALL");
    }

    private static String activityProtocol(Map<String,Object> item)
    {
        String value = String.valueOf(item.getOrDefault("protocol", "Unknown")).trim();
        return value.isBlank() ? "Unknown" : value;
    }

    private static String activitySystem(Map<String,Object> item)
    {
        String value = String.valueOf(item.getOrDefault("system", "")).trim();
        return value.isBlank() ? String.valueOf(item.getOrDefault("channel", "")).trim() : value;
    }

    private static String activityChannelKind(Map<String,Object> item)
    {
        String value = String.valueOf(item.getOrDefault("channelKind", "")).toUpperCase();
        if("TRUNKED".equals(value) || "CONVENTIONAL".equals(value)) { return value; }
        String protocol = activityProtocol(item).toUpperCase();
        return protocol.contains("NBFM") || protocol.equals("AM") ? "CONVENTIONAL" : "TRUNKED";
    }

    private static String activityIdentity(Map<String,Object> item, String preferredField, String fallbackField)
    {
        Object preferred = item.get(preferredField);
        if(preferred != null && !String.valueOf(preferred).isBlank()) { return String.valueOf(preferred); }
        return String.valueOf(item.getOrDefault(fallbackField, "")).trim();
    }

    private static String activityAction(Map<String,Object> item)
    {
        String type = String.valueOf(item.getOrDefault("type", "")).toUpperCase();
        String details = String.valueOf(item.getOrDefault("details", "")).toUpperCase();
        String combined = type + " " + details;
        if(type.contains("CALL")) { return "ACTIVE"; }
        if(combined.contains("EMERGENCY")) { return "EMERGENCY"; }
        if(type.contains("AFFIL") || combined.contains("JOIN")) { return "JOIN"; }
        if(type.contains("DEREGISTER") || combined.contains("LOGOUT")) { return "LOGOUT"; }
        if(type.contains("REGISTER")) { return "REGISTER"; }
        if(type.contains("DATA") || type.contains("PACKET") || type.contains("MESSAGE")) { return "DATA"; }
        if(type.contains("GPS") || type.contains("LRRP") || type.contains("CELLOCATOR")) { return "GPS"; }
        if(type.contains("ACKNOWLEDGE")) { return "ACKNOWLEDGE"; }
        if(type.contains("RADIO CHECK") || type.contains("QUERY")) { return "CHECK"; }
        if(type.contains("PAGE")) { return "PAGE"; }
        if(type.contains("REGROUP") || combined.contains("PATCH")) { return "PATCH"; }
        if(type.contains("REQUEST")) { return "REQUEST"; }
        if(type.contains("STATUS")) { return "STATUS"; }
        if(combined.contains("DENIED") || combined.contains("DENIAL")) { return "DENIAL"; }
        if(combined.contains("BUSY")) { return "BUSY"; }
        if(combined.contains("QUEUE")) { return "QUEUED"; }
        return "UNKNOWN";
    }

    private static long activityRangeMillis(String range)
    {
        if("6h".equalsIgnoreCase(range)) { return TimeUnit.HOURS.toMillis(6); }
        if("7d".equalsIgnoreCase(range)) { return TimeUnit.DAYS.toMillis(7); }
        return TimeUnit.HOURS.toMillis(24);
    }

    private Map<String,Object> latestChannelActivity(Channel channel)
    {
        for(Map<String,Object> item: mActivity)
        {
            if(channelMatchesActivity(channel, item)) { return item; }
        }
        return Map.of();
    }

    private static boolean channelMatchesActivity(Channel channel, Map<String,Object> item)
    {
        String activityChannel = String.valueOf(item.getOrDefault("channel", ""));
        if(!activityChannel.isBlank() && activityChannel.equalsIgnoreCase(valueOrEmpty(channel.getName())))
        {
            return true;
        }
        String activitySystem = String.valueOf(item.getOrDefault("system", ""));
        String activitySite = String.valueOf(item.getOrDefault("site", ""));
        if(!activitySystem.isBlank() && activitySystem.equalsIgnoreCase(valueOrEmpty(channel.getSystem())) &&
            (activitySite.isBlank() || activitySite.equalsIgnoreCase(valueOrEmpty(channel.getSite()))))
        {
            if(channel.getSourceConfiguration() instanceof SourceConfigTuner source)
            {
                long eventFrequency = numberValue(item.get("frequency"));
                return eventFrequency <= 0 || source.getFrequency() == eventFrequency;
            }
            return true;
        }
        return false;
    }

    private static String channelKind(Channel channel)
    {
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn)
        {
            return nxdn.isConventional() ? "CONVENTIONAL" : "TRUNKED";
        }
        DecoderType decoder = channel.getDecodeConfiguration() != null ?
            channel.getDecodeConfiguration().getDecoderType() : null;
        return decoder == DecoderType.AM || decoder == DecoderType.NBFM ? "CONVENTIONAL" : "TRUNKED";
    }

    private void channels(HttpExchange exchange) throws IOException
    {
        if("POST".equals(exchange.getRequestMethod())) { editChannel(exchange); return; }
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        List<Map<String, Object>> result = new ArrayList<>();
        for(Channel channel: new ArrayList<>(mPlaylistManager.getChannelModel().getChannels()))
        {
            result.add(channelMap(channel));
        }
        for(Channel channel: new ArrayList<>(mPlaylistManager.getChannelModel().trafficChannelList()))
        {
            result.add(channelMap(channel));
        }
        json(exchange, 200, result);
    }

    private Map<String, Object> channelMap(Channel channel)
    {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("name", channel.getName());
        value.put("id", channel.getChannelID());
        value.put("system", channel.getSystem());
        value.put("site", channel.getSite());
        value.put("type", channel.getChannelType().name());
        value.put("processing", channel.isProcessing());
        value.put("autoStart", channel.isAutoStart());
        value.put("aliasList", channel.getAliasListName());
        value.put("decoder", channel.getDecodeConfiguration() != null ?
            channel.getDecodeConfiguration().getDecoderType().name() : null);
        value.put("recordUnknownSimplexCalls", channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn &&
            nxdn.isRecordUnknownSimplexCalls());
        value.put("nxdnChannelMode", channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn ?
            nxdn.getChannelMode().name() : null);
        value.put("dcsMonitor", channel.getAuxDecodeConfiguration() != null &&
            channel.getAuxDecodeConfiguration().getAuxDecoders().contains(DecoderType.DCS));
        value.put("eventLoggers", channel.getEventLogConfiguration() != null ?
            channel.getEventLogConfiguration().getLoggers().stream().map(EventLogType::name).toList() : List.of());
        value.put("recorders", channel.getRecordConfiguration() != null ?
            channel.getRecordConfiguration().getRecorders().stream().map(RecorderType::name).toList() : List.of());
        value.put("trafficChannelPoolSize", trafficChannelPoolSize(channel));
        value.put("ignoreDataCalls", ignoreDataCalls(channel));
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm)
        {
            value.put("nbfmBandwidth", nbfm.getBandwidth().name());
            value.put("nbfmTalkgroup", nbfm.getTalkgroup());
            value.put("nbfmAudioFilter", nbfm.isAudioFilter());
            value.put("nbfmAudioALC", nbfm.isAudioALC());
            value.put("nbfmDeemphasis", nbfm.getDeemphasis().name());
            SquelchDecoderConfig squelch = nbfm.getSquelchDecoders().isEmpty() ? null :
                nbfm.getSquelchDecoders().getFirst();
            value.put("nbfmSquelchType", squelch != null ? squelch.getSquelchType().name() :
                SquelchDecoderConfig.SquelchType.NONE.name());
            value.put("nbfmSquelchValue", squelch != null ? squelch.getValue() : "");
        }
        value.put("source", channel.getSourceConfiguration() != null ?
            channel.getSourceConfiguration().toString() : null);
        if(channel.getSourceConfiguration() instanceof SourceConfigTuner tunerSource)
        {
            value.put("frequency", tunerSource.getFrequency());
            value.put("preferredTuner", valueOrEmpty(tunerSource.getPreferredTuner()));
        }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn)
        {
            value.put("nxdnTransmissionMode", nxdn.getTransmissionMode().name());
            value.put("nxdnEncoding", nxdn.getEncoding().name());
            value.put("ignoreEncryptedCalls", nxdn.isIgnoreEncryptedCalls());
        }
        return value;
    }

    private static Integer trafficChannelPoolSize(Channel channel)
    {
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN config) { return config.getTrafficChannelPoolSize(); }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigDMR config) { return config.getTrafficChannelPoolSize(); }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigP25 config) { return config.getTrafficChannelPoolSize(); }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigMPT1327 config) { return config.getTrafficChannelPoolSize(); }
        return null;
    }

    private static boolean ignoreDataCalls(Channel channel)
    {
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN config) { return config.isIgnoreDataCalls(); }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigDMR config) { return config.getIgnoreDataCalls(); }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigP25 config) { return config.getIgnoreDataCalls(); }
        return false;
    }

    private void editChannel(HttpExchange exchange) throws IOException
    {
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        String action = stringValue(request, "action", "update");
        int id = request.has("id") ? request.get("id").getAsInt() : -1;
        Channel channel = mPlaylistManager.getChannelModel().getChannels().stream()
            .filter(item -> item.getChannelID() == id).findFirst().orElse(null);

        if("create".equalsIgnoreCase(action))
        {
            DecoderType decoder = decoderValue(request);
            channel = new Channel(stringValue(request, "name", "New Channel"));
            channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoder));
            try
            {
                updateChannelFields(channel, request, false);
            }
            catch(IllegalArgumentException e)
            {
                json(exchange, 400, Map.of("error", e.getMessage()));
                return;
            }
            mPlaylistManager.getChannelModel().addChannel(channel);
            json(exchange, 201, channelMap(channel));
            return;
        }

        if(channel == null) { json(exchange, 404, Map.of("error", "channel not found")); return; }
        if(channel.isProcessing()) { json(exchange, 409, Map.of("error", "stop channel before editing")); return; }
        if("delete".equalsIgnoreCase(action))
        {
            mPlaylistManager.getChannelModel().removeChannel(channel);
            json(exchange, 200, Map.of("deleted", id));
            return;
        }
        try
        {
            updateChannelFields(channel, request, true);
        }
        catch(IllegalArgumentException e)
        {
            json(exchange, 400, Map.of("error", e.getMessage()));
            return;
        }
        mPlaylistManager.getChannelModel().receive(new ChannelEvent(channel,
            ChannelEvent.Event.NOTIFICATION_CONFIGURATION_CHANGE));
        json(exchange, 200, channelMap(channel));
    }

    private void updateChannelFields(Channel channel, JsonObject request, boolean allowDecoderChange)
    {
        if(request.has("name")) { channel.setName(request.get("name").getAsString()); }
        if(request.has("system")) { channel.setSystem(request.get("system").getAsString()); }
        if(request.has("site")) { channel.setSite(request.get("site").getAsString()); }
        if(request.has("aliasList")) { channel.setAliasListName(request.get("aliasList").getAsString()); }
        if(request.has("autoStart")) { channel.setAutoStart(request.get("autoStart").getAsBoolean()); }
        if(request.has("frequency"))
        {
            SourceConfigTuner source = channel.getSourceConfiguration() instanceof SourceConfigTuner existing ?
                existing : new SourceConfigTuner();
            source.setFrequency(request.get("frequency").getAsLong());
            channel.setSourceConfiguration(source);
        }
        if(request.has("preferredTuner") && channel.getSourceConfiguration() instanceof SourceConfigTuner source)
        {
            String preferred = request.get("preferredTuner").getAsString().trim();
            source.setPreferredTuner(preferred.isBlank() ? null : preferred);
        }
        if(allowDecoderChange && request.has("decoder"))
        {
            DecoderType decoder = decoderValue(request);
            if(channel.getDecodeConfiguration() == null || channel.getDecodeConfiguration().getDecoderType() != decoder)
            {
                channel.setDecodeConfiguration(DecoderFactory.getDecodeConfiguration(decoder));
            }
        }
        if(request.has("recordUnknownSimplexCalls") &&
                channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn)
        {
            nxdn.setRecordUnknownSimplexCalls(request.get("recordUnknownSimplexCalls").getAsBoolean());
        }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn)
        {
            if(request.has("nxdnTransmissionMode"))
            {
                nxdn.setTransmissionMode(TransmissionMode.valueOf(
                    request.get("nxdnTransmissionMode").getAsString().trim().toUpperCase()));
            }
            if(request.has("nxdnEncoding"))
            {
                nxdn.setEncoding(Encoding.valueOf(request.get("nxdnEncoding").getAsString().trim().toUpperCase()));
            }
            if(request.has("ignoreEncryptedCalls"))
            {
                nxdn.setIgnoreEncryptedCalls(request.get("ignoreEncryptedCalls").getAsBoolean());
            }
        }
        applyTrafficSettings(channel, request);
        if(request.has("nxdnChannelMode") && channel.getDecodeConfiguration() instanceof DecodeConfigNXDN nxdn)
        {
            try
            {
                nxdn.setChannelMode(NXDNChannelMode.valueOf(
                    request.get("nxdnChannelMode").getAsString().trim().toUpperCase()));
            }
            catch(IllegalArgumentException e)
            {
                throw new IllegalArgumentException("Invalid NXDN channel type; expected CONVENTIONAL or TRUNKED");
            }
        }
        if(request.has("dcsMonitor"))
        {
            AuxDecodeConfiguration auxiliary = channel.getAuxDecodeConfiguration();
            if(auxiliary == null)
            {
                auxiliary = new AuxDecodeConfiguration();
                channel.setAuxDecodeConfiguration(auxiliary);
            }
            auxiliary.getAuxDecoders().removeIf(type -> type == DecoderType.DCS);
            if(request.get("dcsMonitor").getAsBoolean())
            {
                auxiliary.addAuxDecoder(DecoderType.DCS);
            }
        }
        if(request.has("eventLoggers") && request.get("eventLoggers").isJsonArray())
        {
            EventLogConfiguration eventLogs = new EventLogConfiguration();
            for(var element: request.getAsJsonArray("eventLoggers"))
            {
                try
                {
                    EventLogType type = EventLogType.valueOf(element.getAsString());
                    if(type != EventLogType.BINARY_MESSAGE && !eventLogs.getLoggers().contains(type))
                    {
                        eventLogs.addLogger(type);
                    }
                }
                catch(Exception e)
                {
                    throw new IllegalArgumentException("Unsupported event logger: " + element.getAsString());
                }
            }
            channel.setEventLogConfiguration(eventLogs.getLoggers().isEmpty() ? null : eventLogs);
        }
        if(request.has("recorders") && request.get("recorders").isJsonArray())
        {
            RecordConfiguration recordings = new RecordConfiguration();
            for(var element: request.getAsJsonArray("recorders"))
            {
                try
                {
                    RecorderType type = RecorderType.valueOf(element.getAsString());
                    if(type != RecorderType.AUDIO && !recordings.getRecorders().contains(type))
                    {
                        recordings.addRecorder(type);
                    }
                }
                catch(Exception e)
                {
                    throw new IllegalArgumentException("Unsupported recorder: " + element.getAsString());
                }
            }
            channel.setRecordConfiguration(recordings);
        }
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNBFM nbfm)
        {
            applyNbfmSettings(nbfm, request);
        }
    }

    static void applyTrafficSettings(Channel channel, JsonObject request)
    {
        Integer poolSize = request.has("trafficChannelPoolSize") ?
            Math.max(0, Math.min(100, request.get("trafficChannelPoolSize").getAsInt())) : null;
        Boolean ignoreData = request.has("ignoreDataCalls") ? request.get("ignoreDataCalls").getAsBoolean() : null;
        if(channel.getDecodeConfiguration() instanceof DecodeConfigNXDN config)
        {
            if(poolSize != null) { config.setTrafficChannelPoolSize(poolSize); }
            if(ignoreData != null) { config.setIgnoreDataCalls(ignoreData); }
        }
        else if(channel.getDecodeConfiguration() instanceof DecodeConfigDMR config)
        {
            if(poolSize != null) { config.setTrafficChannelPoolSize(poolSize); }
            if(ignoreData != null) { config.setIgnoreDataCalls(ignoreData); }
        }
        else if(channel.getDecodeConfiguration() instanceof DecodeConfigP25 config)
        {
            if(poolSize != null) { config.setTrafficChannelPoolSize(poolSize); }
            if(ignoreData != null) { config.setIgnoreDataCalls(ignoreData); }
        }
        else if(channel.getDecodeConfiguration() instanceof DecodeConfigMPT1327 config && poolSize != null)
        {
            config.setTrafficChannelPoolSize(poolSize);
        }
    }

    static void applyNbfmSettings(DecodeConfigNBFM config, JsonObject request)
    {
        if(request.has("nbfmBandwidth"))
        {
            DecodeConfigNBFM.Bandwidth bandwidth = DecodeConfigNBFM.Bandwidth.valueOf(
                request.get("nbfmBandwidth").getAsString());
            if(!bandwidth.isFM())
            {
                throw new IllegalArgumentException("Unsupported NBFM bandwidth: " + bandwidth);
            }
            config.setBandwidth(bandwidth);
        }
        if(request.has("nbfmTalkgroup"))
        {
            config.setTalkgroup(request.get("nbfmTalkgroup").getAsInt());
        }
        if(request.has("nbfmAudioFilter"))
        {
            config.setAudioFilter(request.get("nbfmAudioFilter").getAsBoolean());
        }
        if(request.has("nbfmAudioALC"))
        {
            config.setAudioALC(request.get("nbfmAudioALC").getAsBoolean());
        }
        if(request.has("nbfmDeemphasis"))
        {
            config.setDeemphasis(DeemphasisMode.valueOf(request.get("nbfmDeemphasis").getAsString()));
        }
        if(request.has("nbfmSquelchType"))
        {
            SquelchDecoderConfig.SquelchType type = SquelchDecoderConfig.SquelchType.valueOf(
                request.get("nbfmSquelchType").getAsString());
            List<SquelchDecoderConfig> decoders = new ArrayList<>();
            if(type != SquelchDecoderConfig.SquelchType.NONE)
            {
                String value = type == SquelchDecoderConfig.SquelchType.CTCSS_SEARCH ? null :
                    request.has("nbfmSquelchValue") ?
                    request.get("nbfmSquelchValue").getAsString() : "";
                SquelchDecoderConfig decoder = new SquelchDecoderConfig(type, value);
                if(!decoder.isValid())
                {
                    throw new IllegalArgumentException("Select a valid " + type + " squelch code");
                }
                decoders.add(decoder);
            }
            config.setSquelchDecoders(decoders);
        }
    }

    private void channelOptions(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, String>> bandwidths = new ArrayList<>();
        for(DecodeConfigNBFM.Bandwidth bandwidth: DecodeConfigNBFM.Bandwidth.FM_BANDWIDTHS)
        {
            bandwidths.add(Map.of("value", bandwidth.name(), "label", bandwidth.toString()));
        }
        List<Map<String, String>> deemphasis = new ArrayList<>();
        for(DeemphasisMode mode: DeemphasisMode.values())
        {
            deemphasis.add(Map.of("value", mode.name(), "label", mode.toString()));
        }
        List<Map<String, String>> ctcss = new ArrayList<>();
        for(CTCSSCode code: CTCSSCode.STANDARD_CODES)
        {
            ctcss.add(Map.of("value", code.name(), "label", code.getDisplayString()));
        }
        List<Map<String, String>> dcs = new ArrayList<>();
        for(DCSCode code: DCSCode.STANDARD_CODES)
        {
            dcs.add(Map.of("value", code.name(), "label", code.toString()));
        }
        for(DCSCode code: DCSCode.INVERTED_CODES)
        {
            dcs.add(Map.of("value", code.name(), "label", code.toString()));
        }
        result.put("nbfmBandwidths", bandwidths);
        result.put("nbfmDeemphasis", deemphasis);
        result.put("ctcss", ctcss);
        result.put("dcs", dcs);
        List<String> preferredTuners = new ArrayList<>();
        if(mTunerManager != null)
        {
            for(DiscoveredTuner discovered: mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners())
            {
                if(discovered.hasTuner()) { preferredTuners.add(discovered.getTuner().getPreferredName()); }
            }
            preferredTuners.sort(String.CASE_INSENSITIVE_ORDER);
        }
        result.put("preferredTuners", preferredTuners);
        json(exchange, 200, result);
    }

    private DecoderType decoderValue(JsonObject request)
    {
        try { return DecoderType.valueOf(stringValue(request, "decoder", DecoderType.NBFM.name())); }
        catch(Exception e) { return DecoderType.NBFM; }
    }

    private void tuners(HttpExchange exchange) throws IOException
    {
        if("POST".equals(exchange.getRequestMethod()))
        {
            JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8), JsonObject.class);
            try
            {
                String action = requiredString(request, "action");
                if("add".equalsIgnoreCase(action))
                {
                    String host = requiredString(request, "host").trim();
                    int port = integerValue(request, "port", 1234);
                    long frequency = request.has("frequency") ? request.get("frequency").getAsLong() : 100_000_000L;
                    long sampleRate = request.has("sampleRate") ? request.get("sampleRate").getAsLong() : 2_048_000L;
                    if(port < 1 || port > 65535) { throw new IllegalArgumentException("port must be from 1 to 65535"); }
                    if(frequency < 1_000_000L || frequency > 3_000_000_000L)
                    { throw new IllegalArgumentException("frequency must be from 1 MHz to 3 GHz"); }
                    if(sampleRate < 230_400L || sampleRate > 2_880_000L)
                    { throw new IllegalArgumentException("sample rate must be from 230400 to 2880000"); }
                    String id = "RTL TCP " + host + ":" + port;
                    if(mTunerManager.getDiscoveredTunerModel().getDiscoveredTuner(id) != null)
                    { throw new IllegalArgumentException("network tuner already exists"); }
                    NetworkTunerConfiguration configuration = NetworkTunerConfiguration.create();
                    configuration.setHost(host);
                    configuration.setPort(port);
                    configuration.setFrequency(frequency);
                    configuration.setSampleRate(sampleRate);
                    configuration.setGainAuto(booleanValue(request, "gainAuto", true));
                    configuration.setGain(integerValue(request, "gain", 0));
                    configuration.setBiasTEnabled(booleanValue(request, "biasT", false));
                    configuration.setAutoReconnect(booleanValue(request, "autoReconnect", true));
                    mTunerManager.addNetworkTuner(configuration);
                    json(exchange, 201, Map.of("added", id));
                    return;
                }

                String id = requiredString(request, "id");
                DiscoveredTuner discovered = mTunerManager.getDiscoveredTunerModel().getDiscoveredTuner(id);
                if(discovered == null) { json(exchange, 404, Map.of("error", "tuner not found")); return; }
                if("enable".equalsIgnoreCase(action)) { discovered.setEnabled(true); }
                else if("disable".equalsIgnoreCase(action)) { discovered.setEnabled(false); }
                else if("restart".equalsIgnoreCase(action))
                {
                    if(discovered.hasTuner() && discovered.getTuner() instanceof NetworkTuner networkTuner)
                    { networkTuner.getTunerController().reconnect(); }
                    else { discovered.restart(); }
                }
                else if("update".equalsIgnoreCase(action))
                {
                    if(!(discovered.getTunerConfiguration() instanceof NetworkTunerConfiguration configuration))
                    { throw new IllegalArgumentException("only rtl_tcp network tuners can be edited"); }
                    String host = requiredString(request, "host").trim();
                    int port = integerValue(request, "port", configuration.getPort());
                    long frequency = request.has("frequency") ? request.get("frequency").getAsLong() :
                        configuration.getFrequency();
                    long sampleRate = request.has("sampleRate") ? request.get("sampleRate").getAsLong() :
                        configuration.getSampleRate();
                    validateNetworkTuner(host, port, frequency, sampleRate);
                    String newId = "RTL TCP " + host + ":" + port;
                    DiscoveredTuner duplicate = mTunerManager.getDiscoveredTunerModel().getDiscoveredTuner(newId);
                    if(duplicate != null && duplicate != discovered)
                    { throw new IllegalArgumentException("network tuner already exists"); }

                    boolean addressChanged = !host.equals(configuration.getHost()) || port != configuration.getPort();
                    boolean sampleRateChanged = sampleRate != configuration.getSampleRate();
                    boolean frequencyChanged = frequency != configuration.getFrequency();
                    int channelCount = tunerChannelCount(discovered);
                    if(channelCount > 0 && (addressChanged || sampleRateChanged || frequencyChanged))
                    {
                        throw new IllegalArgumentException("stop the " + channelCount +
                            " active channel(s) before changing the network address, sample rate, or center frequency");
                    }

                    String oldId = discovered.getId();
                    if(addressChanged && discovered.hasTuner()) { discovered.stop(); }
                    configuration.setHost(host);
                    configuration.setPort(port);
                    configuration.setFrequency(frequency);
                    configuration.setSampleRate(sampleRate);
                    configuration.setGainAuto(booleanValue(request, "gainAuto", configuration.isGainAuto()));
                    configuration.setGain(integerValue(request, "gain", configuration.getGain()));
                    configuration.setBiasTEnabled(booleanValue(request, "biasT", configuration.isBiasTEnabled()));
                    configuration.setAutoReconnect(booleanValue(request, "autoReconnect", configuration.isAutoReconnect()));
                    if(addressChanged && !discovered.isEnabled())
                    {
                        mTunerManager.getTunerConfigurationManager().updateDisabledTunerIdentity(
                            discovered.getTunerClass(), oldId, newId);
                    }
                    mTunerManager.getTunerConfigurationManager().saveConfigurations();

                    if(addressChanged)
                    {
                        if(discovered.isEnabled())
                        {
                            discovered.restart();
                            if(!discovered.hasTuner()) { discovered.start(); }
                        }
                    }
                    else if(discovered.hasTuner() && discovered.getTuner() instanceof NetworkTuner networkTuner)
                    {
                        NetworkTunerController controller = networkTuner.getTunerController();
                        if(frequencyChanged) { controller.setFrequency(frequency); }
                        if(sampleRateChanged) { controller.setSampleRate(sampleRate); }
                        controller.setGainAuto(configuration.isGainAuto());
                        controller.setGain(configuration.getGain());
                        controller.setBiasTEnabled(configuration.isBiasTEnabled());
                        controller.setAutoReconnect(configuration.isAutoReconnect());
                    }
                    json(exchange, 200, Map.of("updated", newId, "action", action));
                    return;
                }
                else if("tune".equalsIgnoreCase(action))
                {
                    if(!discovered.hasTuner()) { throw new IllegalArgumentException("tuner is not active"); }
                    int channelCount = tunerChannelCount(discovered);
                    if(channelCount > 0)
                    { throw new IllegalArgumentException("stop the " + channelCount + " active channel(s) before tuning"); }
                    long frequency = request.has("frequency") ? request.get("frequency").getAsLong() : 0L;
                    if(!discovered.getTuner().getTunerController().canTune(frequency))
                    { throw new IllegalArgumentException("frequency is outside this tuner's supported range"); }
                    discovered.getTuner().getTunerController().setFrequency(frequency);
                    if(discovered.hasTunerConfiguration())
                    {
                        discovered.getTunerConfiguration().setFrequency(frequency);
                        mTunerManager.getTunerConfigurationManager().saveConfigurations();
                    }
                }
                else if("configure".equalsIgnoreCase(action))
                {
                    if(!discovered.hasTuner()) { throw new IllegalArgumentException("tuner is not active"); }
                    Tuner tuner = discovered.getTuner();
                    long frequency = request.has("frequency") ? request.get("frequency").getAsLong() :
                        tuner.getTunerController().getFrequency();
                    double correction = request.has("frequencyCorrection") ?
                        request.get("frequencyCorrection").getAsDouble() :
                        tuner.getTunerController().getFrequencyCorrection();
                    boolean frequencyChanged = frequency != tuner.getTunerController().getFrequency();
                    int channelCount = tunerChannelCount(discovered);
                    if(frequencyChanged && channelCount > 0)
                    { throw new IllegalArgumentException("stop the " + channelCount + " active channel(s) before tuning"); }
                    if(!tuner.getTunerController().canTune(frequency))
                    { throw new IllegalArgumentException("frequency is outside this tuner's supported range"); }
                    if(frequencyChanged) { tuner.getTunerController().setFrequency(frequency); }
                    tuner.getTunerController().setFrequencyCorrection(correction);

                    if(tuner.getTunerController() instanceof RTL2832TunerController rtlController &&
                        discovered.getTunerConfiguration() instanceof RTL2832TunerConfiguration rtlConfiguration)
                    {
                        RTL2832TunerController.SampleRate selectedRate = rtlConfiguration.getSampleRate();
                        if(request.has("sampleRate"))
                        {
                            long requestedRate = request.get("sampleRate").getAsLong();
                            for(RTL2832TunerController.SampleRate rate: RTL2832TunerController.SampleRate.values())
                            {
                                if(rate.getRate() == requestedRate) { selectedRate = rate; break; }
                            }
                            if(selectedRate.getRate() != requestedRate)
                            { throw new IllegalArgumentException("unsupported RTL-SDR sample rate"); }
                        }
                        if(selectedRate.getRate() != Math.round(rtlController.getSampleRate()))
                        {
                            if(channelCount > 0)
                            { throw new IllegalArgumentException("stop the active channels before changing sample rate"); }
                            rtlController.setSampleRate(selectedRate);
                        }
                        boolean biasT = booleanValue(request, "biasT", rtlConfiguration.isBiasT());
                        rtlController.setBiasT(biasT);
                        rtlConfiguration.setSampleRate(selectedRate);
                        rtlConfiguration.setBiasT(biasT);

                        if(request.has("masterGain") && rtlConfiguration instanceof R8xTunerConfiguration r8x &&
                            rtlController.getEmbeddedTuner() instanceof R8xEmbeddedTuner embeddedTuner)
                        {
                            R8xEmbeddedTuner.MasterGain gain = R8xEmbeddedTuner.MasterGain.valueOf(
                                request.get("masterGain").getAsString());
                            embeddedTuner.setGain(gain, true);
                            r8x.setMasterGain(gain);
                        }
                    }
                    if(discovered.hasTunerConfiguration())
                    {
                        discovered.getTunerConfiguration().setFrequency(frequency);
                        discovered.getTunerConfiguration().setFrequencyCorrection(correction);
                        mTunerManager.getTunerConfigurationManager().saveConfigurations();
                    }
                }
                else if("log".equalsIgnoreCase(action)) { discovered.logState(); }
                else if("delete".equalsIgnoreCase(action))
                {
                    if(!mTunerManager.removeNetworkTuner(id))
                    { json(exchange, 400, Map.of("error", "only network tuners can be deleted")); return; }
                }
                else { throw new IllegalArgumentException("action must be add, update, configure, tune, log, enable, disable, restart, or delete"); }
                json(exchange, 200, Map.of("updated", id, "action", action));
            }
            catch(Exception e)
            {
                json(exchange, 400, Map.of("error", e.getMessage() != null ? e.getMessage() : "invalid tuner request"));
            }
            return;
        }
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        List<Map<String, Object>> result = new ArrayList<>();
        for(DiscoveredTuner discovered: mTunerManager.getDiscoveredTunerModel().getDiscoveredTuners())
        {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("id", discovered.getId());
            value.put("status", discovered.getTunerStatus().name());
            value.put("class", discovered.getTunerClass().name());
            value.put("error", discovered.getErrorMessage());
            value.put("enabled", discovered.isEnabled());
            value.put("network", discovered instanceof DiscoveredNetworkTuner);
            if(discovered.hasTuner())
            {
                Tuner tuner = discovered.getTuner();
                value.put("name", tuner.getPreferredName());
                value.put("type", tuner.getTunerType().name());
                value.put("frequency", tuner.getTunerController().getFrequency());
                value.put("sampleRate", tuner.getTunerController().getSampleRate());
                value.put("frequencyCorrection", tuner.getTunerController().getFrequencyCorrection());
                value.put("minimumFrequency", tuner.getTunerController().getMinimumFrequency());
                value.put("maximumFrequency", tuner.getTunerController().getMaximumFrequency());
                value.put("usableBandwidth", tuner.getTunerController().getUsableBandwidth());
                value.put("sampleRateLocked", tuner.getTunerController().isLockedSampleRate());
                value.put("channelCount", tunerChannelCount(discovered));
                value.put("channels", tunerChannels(discovered));
                if(tuner.getTunerController() instanceof RTL2832TunerController rtlController)
                {
                    value.put("rtl2832", true);
                    value.put("biasT", rtlController.isBiasT());
                    List<Map<String,Object>> sampleRates = new ArrayList<>();
                    for(RTL2832TunerController.SampleRate rate: RTL2832TunerController.SampleRate.values())
                    { sampleRates.add(Map.of("value", rate.getRate(), "label", rate.toString())); }
                    value.put("sampleRates", sampleRates);
                    if(discovered.getTunerConfiguration() instanceof R8xTunerConfiguration r8x)
                    {
                        value.put("masterGain", r8x.getMasterGain().name());
                        List<Map<String,Object>> gains = new ArrayList<>();
                        for(R8xEmbeddedTuner.MasterGain gain: R8xEmbeddedTuner.MasterGain.values())
                        { gains.add(Map.of("value", gain.name(), "label", gain.toString())); }
                        value.put("masterGains", gains);
                    }
                }
                if(tuner instanceof NetworkTuner networkTuner)
                {
                    NetworkTunerController controller = networkTuner.getTunerController();
                    value.put("connection", controller.getConnectionState().name());
                    value.put("connectionError", controller.getLastConnectionError());
                    value.put("reconnectAttempt", controller.getReconnectAttempt());
                }
            }
            else if(discovered.getTunerConfiguration() instanceof NetworkTunerConfiguration configuration)
            {
                value.put("name", discovered.getId());
                value.put("type", "RTL_TCP");
                value.put("frequency", configuration.getFrequency());
                value.put("sampleRate", configuration.getSampleRate());
                value.put("connection", "STOPPED");
            }
            if(discovered.getTunerConfiguration() instanceof NetworkTunerConfiguration configuration)
            {
                value.put("host", configuration.getHost());
                value.put("port", configuration.getPort());
                value.put("gainAuto", configuration.isGainAuto());
                value.put("gain", configuration.getGain());
                value.put("biasT", configuration.isBiasTEnabled());
                value.put("autoReconnect", configuration.isAutoReconnect());
            }
            result.add(value);
        }
        json(exchange, 200, result);
    }

    private void spectrum(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        json(exchange, 200, mSpectrumService.snapshot(mTunerManager, queryValue(exchange, "tuner"),
            queryValue(exchange, "fft"), queryValue(exchange, "window")));
    }

    private static void validateNetworkTuner(String host, int port, long frequency, long sampleRate)
    {
        if(host == null || host.isBlank()) { throw new IllegalArgumentException("host is required"); }
        if(port < 1 || port > 65535) { throw new IllegalArgumentException("port must be from 1 to 65535"); }
        if(frequency < 1_000_000L || frequency > 3_000_000_000L)
        { throw new IllegalArgumentException("frequency must be from 1 MHz to 3 GHz"); }
        if(sampleRate < 230_400L || sampleRate > 2_880_000L)
        { throw new IllegalArgumentException("sample rate must be from 230400 to 2880000"); }
    }

    private static int tunerChannelCount(DiscoveredTuner discovered)
    {
        return discovered.hasTuner() && discovered.getTuner().getChannelSourceManager() != null ?
            discovered.getTuner().getChannelSourceManager().getTunerChannelCount() : 0;
    }

    private static List<Map<String,Object>> tunerChannels(DiscoveredTuner discovered)
    {
        List<Map<String,Object>> result = new ArrayList<>();
        if(discovered.hasTuner() && discovered.getTuner().getChannelSourceManager() != null)
        {
            for(TunerChannel channel: new ArrayList<>(discovered.getTuner().getChannelSourceManager().getTunerChannels()))
            {
                result.add(Map.of("frequency", channel.getFrequency(), "bandwidth", channel.getBandwidth()));
            }
        }
        return result;
    }

    private void broadcasters(HttpExchange exchange) throws IOException
    {
        BroadcastModel model = mPlaylistManager.getBroadcastModel();
        if("POST".equals(exchange.getRequestMethod()))
        {
            try
            {
                JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8), JsonObject.class);
                String action = requiredString(request, "action");
                String lookupName = stringValue(request, "originalName", stringValue(request, "name", null));
                BroadcastConfiguration configuration = model.getBroadcastConfigurations().stream()
                    .filter(item -> lookupName != null && lookupName.equals(item.getName())).findFirst().orElse(null);
                if("create".equalsIgnoreCase(action))
                {
                    BroadcastServerType type = BroadcastServerType.valueOf(requiredString(request, "type"));
                    if(type == BroadcastServerType.REMOTE_CALL_API || type == BroadcastServerType.UNKNOWN)
                    {
                        throw new IllegalArgumentException("Use the Remote Call API editor for this destination type");
                    }
                    configuration = BroadcastFactory.getConfiguration(type, BroadcastFormat.MP3);
                    if(configuration == null) { throw new IllegalArgumentException("Unsupported destination type"); }
                    applyBroadcastConfiguration(configuration, request, true);
                    ConfiguredBroadcast created = model.addBroadcastConfiguration(configuration);
                    mPlaylistManager.schedulePlaylistSave();
                    json(exchange, 201, broadcasterMap(created));
                    return;
                }
                if(configuration == null || configuration instanceof RemoteApiConfiguration)
                {
                    json(exchange, 404, Map.of("error", "stream not found"));
                    return;
                }
                if("clone".equalsIgnoreCase(action))
                {
                    ConfiguredBroadcast cloned = model.cloneBroadcastConfiguration(configuration);
                    mPlaylistManager.schedulePlaylistSave();
                    json(exchange, 201, broadcasterMap(cloned));
                    return;
                }
                if("delete".equalsIgnoreCase(action))
                {
                    removeAliasDestination(configuration.getName());
                    model.removeBroadcastConfiguration(configuration);
                    mPlaylistManager.schedulePlaylistSave();
                    json(exchange, 200, Map.of("deleted", lookupName));
                    return;
                }
                if("update".equalsIgnoreCase(action))
                {
                    String previousName = configuration.getName();
                    applyBroadcastConfiguration(configuration, request, false);
                    if(!previousName.equals(configuration.getName()))
                    {
                        renameAliasDestination(previousName, configuration.getName());
                    }
                    model.process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                    mPlaylistManager.schedulePlaylistSave();
                    json(exchange, 200, broadcasterMap(findConfiguredBroadcast(configuration)));
                    return;
                }
                if("enable".equalsIgnoreCase(action)) { configuration.setEnabled(true); }
                else if("disable".equalsIgnoreCase(action)) { configuration.setEnabled(false); }
                else if(!"restart".equalsIgnoreCase(action))
                {
                    throw new IllegalArgumentException(
                        "action must be create, update, delete, enable, disable, restart, or clone");
                }
                model.process(new BroadcastEvent(configuration, BroadcastEvent.Event.CONFIGURATION_CHANGE));
                mPlaylistManager.schedulePlaylistSave();
                json(exchange, 200, Map.of("updated", configuration.getName(), "action", action));
                return;
            }
            catch(IllegalArgumentException e)
            {
                json(exchange, 400, Map.of("error", e.getMessage() != null ? e.getMessage() : "invalid stream"));
                return;
            }
        }
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        List<Map<String, Object>> result = new ArrayList<>();
        for(ConfiguredBroadcast configured: new ArrayList<>(model.getConfiguredBroadcasts()))
        {
            if(!(configured.getBroadcastConfiguration() instanceof RemoteApiConfiguration))
            {
                result.add(broadcasterMap(configured));
            }
        }
        json(exchange, 200, result);
    }

    private ConfiguredBroadcast findConfiguredBroadcast(BroadcastConfiguration configuration)
    {
        return mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts().stream()
            .filter(item -> item.getBroadcastConfiguration() == configuration).findFirst().orElse(null);
    }

    private Map<String,Object> broadcasterMap(ConfiguredBroadcast configured)
    {
        BroadcastConfiguration configuration = configured.getBroadcastConfiguration();
        AbstractAudioBroadcaster broadcaster = configured.getAudioBroadcaster();
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("name", configuration.getName());
        value.put("type", configuration.getBroadcastServerType().name());
        value.put("typeLabel", configuration.getBroadcastServerType().toString());
        value.put("format", configuration.getBroadcastFormat().name());
        value.put("enabled", configuration.isEnabled());
        value.put("valid", configuration.isValid());
        value.put("state", configured.broadcastStateProperty().get() != null ?
            configured.broadcastStateProperty().get().name() : null);
        value.put("host", configuration.getHost());
        value.put("port", configuration.getPort());
        value.put("delaySeconds", configuration.getDelay() / 1000L);
        value.put("maximumRecordingAgeSeconds", configuration.getMaximumRecordingAge() / 1000L);
        value.put("passwordConfigured", configuration.getPassword() != null && !configuration.getPassword().isBlank());
        int queued = broadcaster != null ? broadcaster.getAudioQueueSize() :
            configuration instanceof RdioScannerConfiguration rdio ?
                RdioScannerBroadcaster.getPersistedQueueSize(rdio,
                    mUserPreferences.getDirectoryPreference().getDirectoryStreaming()) : 0;
        value.put("queue", queued);
        AbstractAudioBroadcaster.BroadcastStatistics saved = broadcaster == null ?
            AbstractAudioBroadcaster.getPersistedStatistics(configuration) : null;
        value.put("sent", broadcaster != null ? broadcaster.getStreamedAudioCount() : saved.streamed());
        value.put("duplicateRejected", broadcaster != null ? broadcaster.getDuplicateRejectedAudioCount() :
            saved.duplicateRejected());
        value.put("errors", broadcaster != null ? broadcaster.getAudioErrorCount() : saved.errors());
        value.put("agedOff", broadcaster != null ? broadcaster.getAgedOffAudioCount() : saved.agedOff());

        if(configuration instanceof RdioScannerConfiguration rdio)
        {
            value.put("systemId", rdio.getSystemID());
            value.put("apiKeyConfigured", rdio.getApiKey() != null && !rdio.getApiKey().isBlank());
            value.put("heartbeatEnabled", rdio.isHeartbeatEnabled());
            value.put("heartbeatIntervalSeconds", rdio.getHeartbeatIntervalSeconds());
            value.put("heartbeatUrl", rdio.getHeartbeatUrl());
        }
        else if(configuration instanceof BroadcastifyCallConfiguration calls)
        {
            value.put("systemId", calls.getSystemID());
            value.put("apiKeyConfigured", calls.getApiKey() != null && !calls.getApiKey().isBlank());
            value.put("testEnabled", calls.isTestEnabled());
            value.put("testInterval", calls.getTestInterval());
            value.put("talkerAliasSource", calls.getTalkerAliasSource().name());
            value.put("storeTalkerAliases", calls.isStoreTalkerAliases());
        }
        else if(configuration instanceof OpenMHzConfiguration openMHz)
        {
            value.put("systemName", openMHz.getSystemName());
            value.put("apiKeyConfigured", openMHz.getApiKey() != null && !openMHz.getApiKey().isBlank());
        }
        if(configuration instanceof IcecastConfiguration icecast)
        {
            value.put("userName", icecast.getUserName());
            value.put("mountPoint", icecast.getMountPoint());
            value.put("description", icecast.getDescription());
            value.put("genre", icecast.getGenre());
            value.put("public", icecast.isPublic());
            value.put("channels", icecast.getChannels());
            value.put("bitRate", icecast.getBitRate());
            value.put("sampleRate", icecast.getSampleRate());
            value.put("siteUrl", icecast.getURL());
            value.put("inline", icecast.getInline());
            if(configuration instanceof BroadcastifyFeedConfiguration feed)
            {
                value.put("feedId", feed.getFeedID());
                value.put("verboseLogging", feed.isVerboseLogging());
            }
        }
        else if(configuration instanceof ShoutcastV1Configuration shoutcast)
        {
            value.put("description", shoutcast.getDescription());
            value.put("genre", shoutcast.getGenre());
            value.put("public", shoutcast.isPublic());
            value.put("channels", shoutcast.getChannels());
            value.put("bitRate", shoutcast.getBitRate());
        }
        else if(configuration instanceof ShoutcastV2Configuration shoutcast)
        {
            value.put("streamId", shoutcast.getStreamID());
            value.put("userId", shoutcast.getUserID());
            value.put("genre", shoutcast.getGenre());
            value.put("public", shoutcast.isPublic());
            value.put("bitRate", shoutcast.getBitRate());
            value.put("siteUrl", shoutcast.getURL());
        }
        return value;
    }

    private void applyBroadcastConfiguration(BroadcastConfiguration configuration, JsonObject request, boolean creating)
    {
        String name = requiredString(request, "name").trim();
        boolean duplicate = mPlaylistManager.getBroadcastModel().getBroadcastConfigurations().stream()
            .anyMatch(item -> item != configuration && name.equals(item.getName()));
        if(duplicate) { throw new IllegalArgumentException("A streaming destination already uses that name"); }
        configuration.setName(name);
        configuration.setHost(requiredString(request, "host").trim());
        configuration.setPort(integerValue(request, "port", configuration.getPort()));
        configuration.setEnabled(booleanValue(request, "enabled", configuration.isEnabled()));
        configuration.setDelay(Math.max(0, integerValue(request, "delaySeconds", 0)) * 1000L);
        configuration.setMaximumRecordingAge(Math.max(1,
            integerValue(request, "maximumRecordingAgeSeconds", 600)) * 1000L);
        String password = stringValue(request, "password", null);
        if(password != null && (!password.isBlank() || creating)) { configuration.setPassword(password); }

        if(configuration instanceof RdioScannerConfiguration rdio)
        {
            String host = configuration.getHost().replaceAll("/+$", "");
            if(!host.endsWith("/api/call-upload")) { host += "/api/call-upload"; }
            configuration.setHost(host);
            rdio.setSystemID(integerValue(request, "systemId", 0));
            String apiKey = stringValue(request, "apiKey", null);
            if(apiKey != null && (!apiKey.isBlank() || creating)) { rdio.setApiKey(apiKey); }
            rdio.setHeartbeatEnabled(booleanValue(request, "heartbeatEnabled", false));
            rdio.setHeartbeatIntervalSeconds(integerValue(request, "heartbeatIntervalSeconds", 60));
            rdio.setHeartbeatUrl(stringValue(request, "heartbeatUrl", null));
        }
        else if(configuration instanceof BroadcastifyCallConfiguration calls)
        {
            calls.setSystemID(integerValue(request, "systemId", 0));
            String apiKey = stringValue(request, "apiKey", null);
            if(apiKey != null && (!apiKey.isBlank() || creating)) { calls.setApiKey(apiKey); }
            calls.setTestEnabled(booleanValue(request, "testEnabled", false));
            calls.setTestInterval(integerValue(request, "testInterval", 15));
            String source = stringValue(request, "talkerAliasSource", calls.getTalkerAliasSource().name());

            try
            {
                calls.setTalkerAliasSource(TalkerAliasSource.valueOf(source));
            }
            catch(IllegalArgumentException iae)
            {
                throw new IllegalArgumentException("Invalid Broadcastify talker alias source: " + source);
            }

            calls.setStoreTalkerAliases(booleanValue(request, "storeTalkerAliases", false));
        }
        else if(configuration instanceof OpenMHzConfiguration openMHz)
        {
            openMHz.setSystemName(requiredString(request, "systemName"));
            String apiKey = stringValue(request, "apiKey", null);
            if(apiKey != null && (!apiKey.isBlank() || creating)) { openMHz.setApiKey(apiKey); }
        }
        if(configuration instanceof IcecastConfiguration icecast)
        {
            icecast.setUserName(stringValue(request, "userName", "source"));
            icecast.setMountPoint(stringValue(request, "mountPoint", "/stream"));
            icecast.setDescription(stringValue(request, "description", null));
            icecast.setGenre(stringValue(request, "genre", null));
            icecast.setPublic(booleanValue(request, "public", false));
            icecast.setChannels(integerValue(request, "channels", 1));
            icecast.setBitRate(integerValue(request, "bitRate", 16));
            icecast.setSampleRate(integerValue(request, "sampleRate", 8000));
            icecast.setURL(stringValue(request, "siteUrl", null));
            icecast.setInline(booleanValue(request, "inline", true));
            if(configuration instanceof BroadcastifyFeedConfiguration feed)
            {
                feed.setFeedID(integerValue(request, "feedId", 0));
                feed.setVerboseLogging(booleanValue(request, "verboseLogging", false));
            }
        }
        else if(configuration instanceof ShoutcastV1Configuration shoutcast)
        {
            shoutcast.setDescription(stringValue(request, "description", null));
            shoutcast.setGenre(stringValue(request, "genre", null));
            shoutcast.setPublic(booleanValue(request, "public", false));
            shoutcast.setChannels(integerValue(request, "channels", 1));
            shoutcast.setBitRate(integerValue(request, "bitRate", 16));
        }
        else if(configuration instanceof ShoutcastV2Configuration shoutcast)
        {
            shoutcast.setStreamID(integerValue(request, "streamId", 1));
            shoutcast.setUserID(stringValue(request, "userId", null));
            shoutcast.setGenre(stringValue(request, "genre", null));
            shoutcast.setPublic(booleanValue(request, "public", true));
            shoutcast.setBitRate(integerValue(request, "bitRate", 16));
            shoutcast.setURL(stringValue(request, "siteUrl", null));
        }
    }

    private void removeAliasDestination(String destination)
    {
        if(destination == null) { return; }
        for(Alias alias: mPlaylistManager.getAliasModel().getAliases())
        {
            new ArrayList<>(alias.getBroadcastChannels()).stream()
                .filter(item -> destination.equals(item.getChannelName())).forEach(alias::removeAliasID);
        }
    }

    private void renameAliasDestination(String previous, String replacement)
    {
        if(previous == null || replacement == null || previous.equals(replacement)) { return; }
        for(Alias alias: mPlaylistManager.getAliasModel().getAliases())
        {
            if(alias.hasBroadcastChannel(previous))
            {
                new ArrayList<>(alias.getBroadcastChannels()).stream()
                    .filter(item -> previous.equals(item.getChannelName())).forEach(alias::removeAliasID);
                if(!alias.hasBroadcastChannel(replacement)) { alias.addAliasID(new BroadcastChannel(replacement)); }
            }
        }
    }

    private void channelControl(HttpExchange exchange) throws IOException
    {
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        String name = request.has("name") ? request.get("name").getAsString() : null;
        String action = request.has("action") ? request.get("action").getAsString() : null;
        Channel channel = mPlaylistManager.getChannelModel().getChannels().stream()
            .filter(item -> item.getName() != null && item.getName().equals(name)).findFirst().orElse(null);
        if(channel == null) { json(exchange, 404, Map.of("error", "channel not found")); return; }
        ChannelProcessingManager manager = mPlaylistManager.getChannelProcessingManager();
        try
        {
            if("start".equalsIgnoreCase(action)) { manager.start(channel); }
            else if("stop".equalsIgnoreCase(action)) { manager.stop(channel); }
            else { json(exchange, 400, Map.of("error", "action must be start or stop")); return; }
            json(exchange, 200, channelMap(channel));
        }
        catch(Exception e)
        {
            json(exchange, 409, Map.of("error", e.getMessage() != null ? e.getMessage() : "channel operation failed"));
        }
    }

    private void remoteDestinations(HttpExchange exchange) throws IOException
    {
        if("GET".equals(exchange.getRequestMethod()))
        {
            List<Map<String,Object>> result = new ArrayList<>();
            for(ConfiguredBroadcast configured: new ArrayList<>(
                mPlaylistManager.getBroadcastModel().getConfiguredBroadcasts()))
            {
                if(configured.getBroadcastConfiguration() instanceof RemoteApiConfiguration configuration)
                {
                    result.add(remoteDestinationMap(configuration));
                }
            }
            json(exchange, 200, result);
            return;
        }
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        try
        {
            JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8), JsonObject.class);
            String action = stringValue(request, "action", "create");
            String originalName = stringValue(request, "originalName", null);
            RemoteApiConfiguration configuration = findRemoteDestination(originalName);
            if("reconnect".equalsIgnoreCase(action))
            {
                if(configuration == null) { json(exchange, 404, Map.of("error", "destination not found")); return; }
                AbstractAudioBroadcaster broadcaster =
                    mPlaylistManager.getBroadcastModel().getBroadcaster(configuration.getName());
                if(!(broadcaster instanceof RemoteApiBroadcaster remote))
                {
                    json(exchange, 409, Map.of("error", "destination is disabled or has not started"));
                    return;
                }
                remote.reconnect();
                json(exchange, 200, remoteDestinationMap(configuration));
                return;
            }
            if("delete".equalsIgnoreCase(action))
            {
                if(configuration == null) { json(exchange, 404, Map.of("error", "destination not found")); return; }
                mPlaylistManager.getBroadcastModel().removeBroadcastConfiguration(configuration);
                json(exchange, 200, Map.of("deleted", originalName));
                return;
            }
            boolean creating = configuration == null;
            if(!creating && !"update".equalsIgnoreCase(action))
            { json(exchange, 409, Map.of("error", "destination already exists")); return; }
            if(creating) { configuration = new RemoteApiConfiguration(); }
            String name = requiredString(request, "name");
            String url = requiredString(request, "url");
            configuration.setName(name);
            configuration.setHost(url);
            configuration.setEnabled(booleanValue(request, "enabled", true));
            String apiKey = stringValue(request, "apiKey", null);
            if(apiKey != null && !apiKey.isBlank())
            {
                configuration.setApiKey(apiKey);
            }
            configuration.setApiKeyEnvironmentVariable(stringValue(request, "apiKeyEnvironmentVariable",
                RemoteApiConfiguration.DEFAULT_API_KEY_ENVIRONMENT_VARIABLE));
            configuration.setAuthenticationHeader(stringValue(request, "authenticationHeader", "Authorization"));
            configuration.setAuthenticationPrefix(stringValue(request, "authenticationPrefix", "Bearer "));
            configuration.setMaximumRetries(integerValue(request, "maximumRetries", 5));
            configuration.setRetryIndefinitely(booleanValue(request, "retryIndefinitely", true));
            configuration.setMaximumConcurrentUploads(integerValue(request, "maximumConcurrentUploads", 2));
            configuration.setRequestTimeoutSeconds(integerValue(request, "requestTimeoutSeconds", 60));
            configuration.setHeartbeatEnabled(booleanValue(request, "heartbeatEnabled", false));
            configuration.setHeartbeatIntervalSeconds(integerValue(request, "heartbeatIntervalSeconds", 60));
            configuration.setOpenAiEnabled(booleanValue(request, "openAiEnabled", false));
            configuration.setTranslateToEnglish(booleanValue(request, "translateToEnglish", false));
            configuration.setOpenAiKeyEnvironmentVariable(stringValue(request, "openAiKeyEnvironmentVariable",
                RemoteApiConfiguration.DEFAULT_OPENAI_KEY_ENVIRONMENT_VARIABLE));
            configuration.setLocalWhisperExecutable(stringValue(request, "localWhisperExecutable", null));
            configuration.setLocalWhisperModel(stringValue(request, "localWhisperModel", null));
            configuration.setMaximumRecordingAge(integerValue(request, "maximumRecordingAgeSeconds", 600) * 1000L);
            if(creating)
            {
                mPlaylistManager.getBroadcastModel().addBroadcastConfiguration(configuration);
            }
            else
            {
                mPlaylistManager.getBroadcastModel().process(new BroadcastEvent(configuration,
                    BroadcastEvent.Event.CONFIGURATION_CHANGE));
            }
            mPlaylistManager.schedulePlaylistSave();
            json(exchange, creating ? 201 : 200, remoteDestinationMap(configuration));
        }
        catch(IllegalArgumentException e)
        {
            json(exchange, 400, Map.of("error", e.getMessage()));
        }
    }

    private RemoteApiConfiguration findRemoteDestination(String name)
    {
        if(name == null) { return null; }
        return mPlaylistManager.getBroadcastModel().getBroadcastConfigurations().stream()
            .filter(RemoteApiConfiguration.class::isInstance).map(RemoteApiConfiguration.class::cast)
            .filter(item -> name.equals(item.getName())).findFirst().orElse(null);
    }

    private Map<String,Object> remoteDestinationMap(RemoteApiConfiguration configuration)
    {
        Map<String,Object> value = new LinkedHashMap<>();
        value.put("name", configuration.getName());
        value.put("type", configuration.getBroadcastServerType().name());
        value.put("typeLabel", configuration.getBroadcastServerType().toString());
        value.put("url", configuration.getHost());
        value.put("enabled", configuration.isEnabled());
        String resolvedApiKey = configuration.resolveApiKey();
        value.put("apiKeyConfigured", resolvedApiKey != null && !resolvedApiKey.isBlank());
        value.put("apiKeyEnvironmentVariable", configuration.getApiKeyEnvironmentVariable());
        value.put("authenticationHeader", configuration.getAuthenticationHeader());
        value.put("authenticationPrefix", configuration.getAuthenticationPrefix());
        value.put("maximumRetries", configuration.getMaximumRetries());
        value.put("retryIndefinitely", configuration.isRetryIndefinitely());
        value.put("maximumConcurrentUploads", configuration.getMaximumConcurrentUploads());
        value.put("requestTimeoutSeconds", configuration.getRequestTimeoutSeconds());
        value.put("maximumRecordingAgeSeconds", configuration.getMaximumRecordingAge() / 1000L);
        value.put("heartbeatEnabled", configuration.isHeartbeatEnabled());
        value.put("heartbeatIntervalSeconds", configuration.getHeartbeatIntervalSeconds());
        value.put("openAiEnabled", configuration.isOpenAiEnabled());
        value.put("translateToEnglish", configuration.isTranslateToEnglish());
        value.put("openAiKeyEnvironmentVariable", configuration.getOpenAiKeyEnvironmentVariable());
        value.put("localWhisperExecutable", configuration.getLocalWhisperExecutable());
        value.put("localWhisperModel", configuration.getLocalWhisperModel());
        AbstractAudioBroadcaster broadcaster = mPlaylistManager.getBroadcastModel().getBroadcaster(configuration.getName());
        AbstractAudioBroadcaster.BroadcastStatistics saved = broadcaster == null ?
            AbstractAudioBroadcaster.getPersistedStatistics(configuration) : null;
        value.put("state", !configuration.isEnabled() ? "Disabled" : broadcaster != null ?
            broadcaster.getBroadcastState().toString() : "Starting");
        value.put("sent", broadcaster != null ? broadcaster.getStreamedAudioCount() : saved.streamed());
        value.put("errors", broadcaster != null ? broadcaster.getAudioErrorCount() : saved.errors());
        value.put("agedOff", broadcaster != null ? broadcaster.getAgedOffAudioCount() : saved.agedOff());
        value.put("queuedCalls", RemoteApiBroadcaster.getPreservedQueueSize(configuration));
        value.put("activeUploads", 0);
        if(broadcaster instanceof RemoteApiBroadcaster remote)
        {
            value.put("lastHeartbeatAttempt", remote.getLastHeartbeatAttempt());
            value.put("lastHeartbeatSuccess", remote.getLastHeartbeatSuccess());
            value.put("lastHeartbeatError", remote.getLastHeartbeatError());
            value.put("lastCallAttempt", remote.getLastCallAttempt());
            value.put("lastCallSuccess", remote.getLastCallSuccess());
            value.put("lastCallError", remote.getLastCallError());
            value.put("lastContactAttempt", remote.getLastContactAttempt());
            value.put("lastContactSuccess", remote.getLastContactSuccess());
            value.put("lastReconnect", remote.getLastReconnect());
            value.put("reconnectCount", remote.getReconnectCount());
            value.put("consecutiveFailures", remote.getConsecutiveConnectionFailures());
            value.put("connectionStale", remote.isConnectionStale());
            value.put("queuedCalls", remote.getAudioQueueSize());
            value.put("activeUploads", remote.getActiveUploadCount());
        }
        return value;
    }

    /** Talkgroup aliases shared by web channel editing, RadioReference supplements and Remote Calls. */
    private void talkgroups(HttpExchange exchange) throws IOException
    {
        if("GET".equals(exchange.getRequestMethod()))
        {
            List<Map<String,Object>> result = new ArrayList<>();
            List<Alias> aliases = new ArrayList<>(mPlaylistManager.getAliasModel().getAliases());
            for(int index = 0; index < aliases.size(); index++)
            {
                Alias alias = aliases.get(index);
                for(AliasID identifier: alias.getAliasIdentifiers())
                {
                    if(identifier instanceof Talkgroup talkgroup)
                    {
                        Map<String,Object> value = new LinkedHashMap<>();
                        value.put("id", index);
                        value.put("name", alias.getName());
                        value.put("group", alias.getGroup());
                        value.put("aliasList", alias.getAliasListName());
                        value.put("protocol", talkgroup.getProtocol().name());
                        value.put("talkgroup", talkgroup.getValue());
                        value.put("record", alias.isRecordable());
                        value.put("priority", alias.getPlaybackPriority());
                        Ctcss ctcss = alias.getAliasIdentifiers().stream().filter(Ctcss.class::isInstance)
                            .map(Ctcss.class::cast).findFirst().orElse(null);
                        value.put("ctcss", ctcss != null && ctcss.isValid() ?
                            ctcss.getCTCSSCode().name() : "");
                        value.put("ctcssLabel", ctcss != null && ctcss.isValid() ?
                            ctcss.getCTCSSCode().getDisplayString() : "");
                        value.put("remoteCalls", alias.getBroadcastChannels().stream()
                            .map(item -> item.getChannelName()).toList());
                        result.add(value);
                    }
                }
            }
            json(exchange, 200, result);
            return;
        }
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        try
        {
            JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
                StandardCharsets.UTF_8), JsonObject.class);
            String action = stringValue(request, "action", "create");
            int id = integerValue(request, "id", -1);
            List<Alias> aliases = mPlaylistManager.getAliasModel().aliasList();
            Alias alias = id >= 0 && id < aliases.size() ? aliases.get(id) : null;
            if("delete".equalsIgnoreCase(action))
            {
                if(alias == null) { json(exchange, 404, Map.of("error", "talkgroup alias not found")); return; }
                mPlaylistManager.getAliasModel().removeAlias(alias);
                json(exchange, 200, Map.of("deleted", id));
                return;
            }
            boolean creating = alias == null;
            if(creating) { alias = new Alias(requiredString(request, "name")); }
            alias.setName(requiredString(request, "name"));
            alias.setAliasListName(requiredString(request, "aliasList"));
            alias.setGroup(stringValue(request, "group", null));
            alias.setRecordable(booleanValue(request, "record", false));
            alias.setCallPriority(integerValue(request, "priority", 100));
            Protocol protocol = Protocol.valueOf(requiredString(request, "protocol"));
            int value = integerValue(request, "talkgroup", -1);
            Talkgroup talkgroup = alias.getAliasIdentifiers().stream().filter(Talkgroup.class::isInstance)
                .map(Talkgroup.class::cast).findFirst().orElse(null);
            if(talkgroup == null) { talkgroup = new Talkgroup(protocol, value); alias.addAliasID(talkgroup); }
            else { talkgroup.setProtocol(protocol); talkgroup.setValue(value); }
            Ctcss ctcss = alias.getAliasIdentifiers().stream().filter(Ctcss.class::isInstance)
                .map(Ctcss.class::cast).findFirst().orElse(null);
            String ctcssValue = stringValue(request, "ctcss", "");
            if(ctcssValue.isBlank())
            {
                alias.removeAliasID(ctcss);
            }
            else
            {
                CTCSSCode code = CTCSSCode.valueOf(ctcssValue);
                if(ctcss == null)
                {
                    ctcss = new Ctcss();
                    alias.addAliasID(ctcss);
                }
                ctcss.setCTCSSCode(code);
                if(!ctcss.isValid())
                {
                    throw new IllegalArgumentException("select a valid CTCSS alias tone");
                }
            }
            alias.removeAllBroadcastChannels();
            if(request.has("remoteCalls") && request.get("remoteCalls").isJsonArray())
            {
                for(var item: request.getAsJsonArray("remoteCalls"))
                {
                    alias.addAliasID(new BroadcastChannel(item.getAsString()));
                }
            }
            if(!talkgroup.isValid()) { throw new IllegalArgumentException("talkgroup is outside the valid range for " + protocol); }
            if(creating) { mPlaylistManager.getAliasModel().addAlias(alias); }
            else
            {
                mPlaylistManager.getAliasModel().removeAlias(alias);
                mPlaylistManager.getAliasModel().addAlias(alias);
            }
            mPlaylistManager.schedulePlaylistSave();
            json(exchange, creating ? 201 : 200, Map.of("saved", alias.getName()));
        }
        catch(Exception e)
        {
            json(exchange, 400, Map.of("error", e.getMessage() != null ? e.getMessage() : "invalid talkgroup"));
        }
    }

    private static String requiredString(JsonObject object, String name)
    {
        String value = stringValue(object, name, null);
        if(value == null || value.isBlank())
        {
            throw new IllegalArgumentException(name + " is required");
        }
        return value;
    }

    private static String stringValue(JsonObject object, String name, String defaultValue)
    {
        return object.has(name) && !object.get(name).isJsonNull() ? object.get(name).getAsString() : defaultValue;
    }

    private static boolean booleanValue(JsonObject object, String name, boolean defaultValue)
    {
        return object.has(name) ? object.get(name).getAsBoolean() : defaultValue;
    }

    private static int integerValue(JsonObject object, String name, int defaultValue)
    {
        return object.has(name) ? object.get(name).getAsInt() : defaultValue;
    }

    private static long longValue(JsonObject object, String name, long defaultValue)
    {
        return object.has(name) ? object.get(name).getAsLong() : defaultValue;
    }

    private static String errorMessage(Throwable error)
    {
        return error.getMessage() != null && !error.getMessage().isBlank() ? error.getMessage() :
            error.getClass().getSimpleName();
    }

    private void dashboard(HttpExchange exchange) throws IOException
    {
        String method = exchange.getRequestMethod();
        if(!"GET".equals(method) && !"HEAD".equals(method)) { methodNotAllowed(exchange); return; }

        String requestPath = exchange.getRequestURI().getPath();
        String resourcePath;
        if("/".equals(requestPath))
        {
            resourcePath = "/web/vce/index.html";
        }
        else if(requestPath.startsWith("/assets/") && !requestPath.contains(".."))
        {
            resourcePath = "/web/vce" + requestPath;
        }
        else if("/LICENSE.txt".equals(requestPath) || "/NOTICE.txt".equals(requestPath))
        {
            resourcePath = "/web/vce" + requestPath;
        }
        else
        {
            json(exchange, 404, Map.of("error", "not found"));
            return;
        }

        byte[] bytes;
        try(InputStream input = SdrTrunkWebServer.class.getResourceAsStream(resourcePath))
        {
            if(input == null)
            {
                json(exchange, 404, Map.of("error", "not found"));
                return;
            }
            bytes = input.readAllBytes();
        }

        exchange.getResponseHeaders().set("Content-Type", webContentType(resourcePath));
        exchange.getResponseHeaders().set("Cache-Control", resourcePath.endsWith("index.html") ?
            "no-store" : "public, max-age=3600");
        exchange.getResponseHeaders().set("X-Sdrtrunk-Web-Revision", VCE_WEB_REVISION);
        exchange.sendResponseHeaders(200, "HEAD".equals(method) ? -1 : bytes.length);
        if(!"HEAD".equals(method))
        {
            exchange.getResponseBody().write(bytes);
        }
        exchange.close();
    }

    private static String webContentType(String path)
    {
        if(path.endsWith(".html")) { return "text/html; charset=UTF-8"; }
        if(path.endsWith(".css")) { return "text/css; charset=UTF-8"; }
        if(path.endsWith(".js")) { return "text/javascript; charset=UTF-8"; }
        if(path.endsWith(".svg")) { return "image/svg+xml"; }
        if(path.endsWith(".png")) { return "image/png"; }
        if(path.endsWith(".webmanifest")) { return "application/manifest+json; charset=UTF-8"; }
        if(path.endsWith(".txt")) { return "text/plain; charset=UTF-8"; }
        return "application/octet-stream";
    }

    @Override
    public Listener<AudioSegment> getAudioSegmentListener()
    {
        return segment ->
        {
            Runnable encode = () -> encodeLiveAudio(segment);
            if(segment.isComplete()) { Thread.startVirtualThread(encode); }
            else
            {
                segment.completeProperty().addListener((observable, oldValue, complete) ->
                {
                    if(complete) { Thread.startVirtualThread(encode); }
                });
            }
        };
    }

    private void encodeLiveAudio(AudioSegment segment)
    {
        try
        {
            Identifier to = segment.getIdentifierCollection().getToIdentifier();
            List<Alias> talkgroupAliases = getRecordableTalkgroupAliases(segment.getAliasList(), to);
            boolean duplicateSuppressed = segment.isDuplicate() &&
                mUserPreferences.getCallManagementPreference().isDuplicateRecordingSuppressionEnabled();

            /*
             * The web listener receives completed decoder audio before the recording manager applies its routing
             * rules.  Require a record-enabled alias on the TO/talkgroup identifier so that source-radio aliases,
             * channel record overrides, and unidentified traffic cannot leak into web playback.
             */
            if(segment.hasAudio() && !segment.isEncrypted() && segment.recordAudioProperty().get() &&
                !segment.isDoNotMonitor() && !duplicateSuppressed && !talkgroupAliases.isEmpty())
            {
                MP3AudioConverter converter = new MP3AudioConverter(InputAudioFormat.SR_8000,
                    MP3Setting.CBR_16, false);
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                for(byte[] block: converter.convert(segment.getAudioBuffers())) { output.write(block); }
                byte[] audio = output.toByteArray();
                if(audio.length > 0)
                {
                    Identifier from = segment.getIdentifierCollection().getFromIdentifier();
                    List<String> aliases = talkgroupAliases.stream().map(Alias::getName)
                        .filter(name -> name != null && !name.isBlank()).toList();
                    Identifier frequency = segment.getIdentifierCollection().getIdentifier(
                        IdentifierClass.CONFIGURATION, Form.CHANNEL_FREQUENCY, Role.ANY);
                    Map<String,Object> metadata = new LinkedHashMap<>();
                    metadata.put("talkgroup", to.toString());
                    metadata.put("alias", aliases.isEmpty() ? "Configured Talkgroup" : String.join(", ", aliases));
                    metadata.put("channel", channelName(segment.getIdentifierCollection()));
                    metadata.put("protocol", to.getProtocol() != null ? to.getProtocol().toString() : "Unknown");
                    metadata.put("source", from != null ? from.toString() : "Unknown");
                    metadata.put("frequency", frequency instanceof ConfigurationLongIdentifier value ?
                        value.getValue() : 0);
                    metadata.put("started", segment.getStartTimestamp());
                    metadata.put("duration", segment.getDuration());
                    metadata.put("audioLevelDbfs", audioLevelDbfs(segment));
                    metadata.put("rfSignalAvailable", false);
                    synchronized(mLiveAudioQueue)
                    {
                        long sequence = mLatestAudioSequence.incrementAndGet();
                        metadata.put("sequence", sequence);
                        mLatestAudioMetadata = Map.copyOf(metadata);
                        mLiveAudioQueue.addLast(new LiveAudioCall(sequence, audio, Map.copyOf(metadata)));
                        while(mLiveAudioQueue.size() > LIVE_AUDIO_QUEUE_LIMIT) { mLiveAudioQueue.pollFirst(); }
                    }
                    mTranscriptionService.submit(audio, new LinkedHashMap<>(metadata));
                }
            }
        }
        catch(Exception e)
        {
            mLog.warn("Unable to prepare live web audio", e);
        }
        finally
        {
            segment.decrementConsumerCount();
        }
    }

    private static List<Alias> getRecordableTalkgroupAliases(AliasList aliasList, Identifier talkgroup)
    {
        if(aliasList == null || talkgroup == null)
        {
            return List.of();
        }

        return aliasList.getAliases(talkgroup).stream()
            .filter(alias -> alias != null && alias.isRecordable())
            .toList();
    }

    private void liveAudio(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        long requested = longQueryValue(exchange, "after", -1);
        LiveAudioCall call = mLiveAudioQueue.stream().filter(item -> item.sequence() > requested)
            .findFirst().orElse(null);
        if(call == null)
        {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
            return;
        }
        long queued = mLiveAudioQueue.stream().filter(item -> item.sequence() > call.sequence()).count();
        exchange.getResponseHeaders().set("X-Audio-Sequence", Long.toString(call.sequence()));
        exchange.getResponseHeaders().set("X-Audio-Queued", Long.toString(queued));
        bytes(exchange, 200, "audio/mpeg", call.audio());
    }

    private void liveStatus(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        long requested = longQueryValue(exchange, "sequence", -1);
        Map<String,Object> source = requested < 0 ? mLatestAudioMetadata : mLiveAudioQueue.stream()
            .filter(item -> item.sequence() == requested).map(LiveAudioCall::metadata)
            .findFirst().orElse(mLatestAudioMetadata);
        Map<String,Object> status = new LinkedHashMap<>(source);
        status.put("queued", mLiveAudioQueue.stream().filter(item -> item.sequence() > requested).count());
        Object level = status.get("audioLevelDbfs");
        if(level instanceof Number number && !Double.isFinite(number.doubleValue()))
        {
            status.put("audioLevelDbfs", -100.0);
        }
        json(exchange, 200, status);
    }

    private record LiveAudioCall(long sequence, byte[] audio, Map<String,Object> metadata) {}

    private static double audioLevelDbfs(AudioSegment segment)
    {
        double sum = 0;
        long count = 0;
        for(float[] buffer: segment.getAudioBuffers())
        {
            for(float sample: buffer)
            {
                if(Float.isFinite(sample))
                {
                    sum += sample * sample;
                    count++;
                }
            }
        }
        if(count == 0 || sum == 0) { return -100.0; }
        double level = 20.0 * Math.log10(Math.sqrt(sum / count));
        return Double.isFinite(level) ? Math.max(-100.0, level) : -100.0;
    }

    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return event ->
        {
            if(event == null || event.getIdentifierCollection() == null)
            {
                return;
            }
            Map<String,Object> item = new LinkedHashMap<>();
            item.put("time", event.getTimeStart());
            item.put("end", event.getTimeEnd());
            item.put("duration", event.getDuration());
            item.put("protocol", event.getProtocol() != null ? event.getProtocol().toString() : "Unknown");
            item.put("type", event.getEventType() != null ? event.getEventType().toString() : "Activity");
            Identifier talkgroup = event.getIdentifierCollection().getToIdentifier();
            AliasList aliasList = mPlaylistManager.getAliasModel().getAliasList(event.getIdentifierCollection());
            List<Alias> talkgroupAliases = aliasList != null && talkgroup != null ?
                aliasList.getAliases(talkgroup).stream().filter(alias -> alias != null).toList() : List.of();

            //Retain all configured, monitored calls for dashboard activity.  Recording is tracked separately so the
            //Calls and Recorded totals remain meaningful while undefined and muted traffic stays excluded.
            if(talkgroupAliases.isEmpty() || talkgroupAliases.stream()
                .anyMatch(alias -> alias.getPlaybackPriority() <= Priority.DO_NOT_MONITOR))
            {
                return;
            }

            List<String> aliases = talkgroupAliases.stream().map(Alias::getName)
                .filter(name -> name != null && !name.isBlank()).toList();
            Identifier source = event.getIdentifierCollection().getFromIdentifier();
            List<String> sourceAliases = aliasList != null && source != null ? aliasList.getAliases(source).stream()
                .map(Alias::getName).filter(name -> name != null && !name.isBlank()).toList() : List.of();
            item.put("talkgroup", talkgroup.toString());
            item.put("talkgroupId", identifierValue(talkgroup));
            item.put("destinationKind", switch(talkgroup.getForm())
            {
                case RADIO -> "radio";
                case PATCH_GROUP -> "patch_group";
                default -> "talkgroup";
            });
            item.put("alias", aliases.isEmpty() ? "Configured Talkgroup" : String.join(", ", aliases));
            item.put("channel", channelName(event.getIdentifierCollection()));
            item.put("system", configurationIdentifierValue(event.getIdentifierCollection(), Form.SYSTEM));
            item.put("site", configurationIdentifierValue(event.getIdentifierCollection(), Form.SITE));
            item.put("source", source != null ? source.toString() : "");
            item.put("sourceId", identifierValue(source));
            item.put("sourceAlias", String.join(", ", sourceAliases));
            item.put("details", event.getDetails());
            item.put("frequency", event.getChannelDescriptor() != null ?
                event.getChannelDescriptor().getDownlinkFrequency() : 0);
            Channel configuredChannel = configuredChannel(event.getIdentifierCollection(), item);
            item.put("channelKind", configuredChannel != null ? channelKind(configuredChannel) :
                activityChannelKind(item));
            item.put("signalAvailable", false);
            item.put("recorded", talkgroupAliases.stream().anyMatch(Alias::isRecordable));
            item.put("id", activityId(item));
            recordActivity(item);
        };
    }

    private void recordActivity(Map<String,Object> item)
    {
        Object id = item.get("id");
        if(id != null)
        {
            mActivity.removeIf(existing -> id.equals(existing.get("id")));
        }
        mActivity.addFirst(new LinkedHashMap<>(item));
        while(mActivity.size() > ACTIVITY_LIMIT) { mActivity.pollLast(); }
        scheduleActivityWrite();
    }

    private static String activityId(Map<String,Object> item)
    {
        String identity = String.valueOf(item.getOrDefault("time", 0)) + "\u0000" +
            item.getOrDefault("talkgroup", "") + "\u0000" + item.getOrDefault("source", "") + "\u0000" +
            item.getOrDefault("channel", "");
        return UUID.nameUUIDFromBytes(identity.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private void scheduleActivityWrite()
    {
        if(mActivityFile != null && mActivityWriteScheduled.compareAndSet(false, true))
        {
            ThreadPool.SCHEDULED.schedule(() ->
            {
                mActivityWriteScheduled.set(false);
                persistActivity();
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    private void loadActivity()
    {
        if(mActivityFile == null || !Files.isRegularFile(mActivityFile)) { return; }
        synchronized(mActivityFileLock)
        {
            try
            {
                JsonArray saved = GSON.fromJson(Files.readString(mActivityFile, StandardCharsets.UTF_8),
                    JsonArray.class);
                if(saved == null) { return; }
                for(var element: saved)
                {
                    if(!element.isJsonObject()) { continue; }
                    @SuppressWarnings("unchecked")
                    Map<String,Object> item = GSON.fromJson(element, Map.class);
                    if(item != null)
                    {
                        item.putIfAbsent("id", activityId(item));
                        mActivity.addLast(new LinkedHashMap<>(item));
                        if(mActivity.size() >= ACTIVITY_LIMIT) { break; }
                    }
                }
                mLog.info("Restored [{}] saved web activity records", mActivity.size());
            }
            catch(Exception e)
            {
                mLog.warn("Unable to restore saved web activity [{}]", mActivityFile, e);
            }
        }
    }

    private void persistActivity()
    {
        if(mActivityFile == null) { return; }
        synchronized(mActivityFileLock)
        {
            Path temporary = mActivityFile.resolveSibling(mActivityFile.getFileName() + ".tmp");
            try
            {
                Files.createDirectories(mActivityFile.getParent());
                Files.writeString(temporary, GSON.toJson(new ArrayList<>(mActivity)), StandardCharsets.UTF_8);
                try
                {
                    Files.move(temporary, mActivityFile, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
                }
                catch(AtomicMoveNotSupportedException e)
                {
                    Files.move(temporary, mActivityFile, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            catch(Exception e)
            {
                mLog.warn("Unable to save web activity [{}]", mActivityFile, e);
            }
        }
    }

    private static String channelName(IdentifierCollection identifiers)
    {
        Identifier channel = identifiers.getIdentifier(IdentifierClass.CONFIGURATION, Form.CHANNEL, Role.ANY);
        if(channel == null)
        {
            channel = identifiers.getIdentifier(IdentifierClass.DECODER, Form.CHANNEL_NAME, Role.BROADCAST);
        }
        return channel != null ? channel.toString() : "";
    }

    private static Object identifierValue(Identifier identifier)
    {
        return identifier != null && identifier.getValue() != null ? identifier.getValue() : "";
    }

    private static String configurationIdentifierValue(IdentifierCollection identifiers, Form form)
    {
        Identifier identifier = identifiers.getIdentifier(IdentifierClass.CONFIGURATION, form, Role.ANY);
        return identifier != null && identifier.getValue() != null ? identifier.getValue().toString() : "";
    }

    private Channel configuredChannel(IdentifierCollection identifiers, Map<String,Object> item)
    {
        if(mPlaylistManager == null) { return null; }
        String name = channelName(identifiers);
        String system = String.valueOf(item.getOrDefault("system", ""));
        String site = String.valueOf(item.getOrDefault("site", ""));
        for(Channel channel: new ArrayList<>(mPlaylistManager.getChannelModel().getChannels()))
        {
            if(!name.isBlank() && name.equalsIgnoreCase(valueOrEmpty(channel.getName()))) { return channel; }
            if(!system.isBlank() && system.equalsIgnoreCase(valueOrEmpty(channel.getSystem())) &&
                (site.isBlank() || site.equalsIgnoreCase(valueOrEmpty(channel.getSite()))))
            {
                return channel;
            }
        }
        return null;
    }

    private void activity(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        int limit = (int)Math.max(1, Math.min(ACTIVITY_LIMIT, longQueryValue(exchange, "limit", 100)));
        json(exchange, 200, mActivity.stream().limit(limit).toList());
    }

    private void transcripts(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        json(exchange, 200, mTranscriptionService.transcripts());
    }

    private void whisperSettings(HttpExchange exchange) throws IOException
    {
        if("GET".equals(exchange.getRequestMethod()))
        {
            json(exchange, 200, mTranscriptionService.settings());
            return;
        }
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        @SuppressWarnings("unchecked")
        Map<String,Object> request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8), Map.class);
        mTranscriptionService.update(request);
        json(exchange, 200, mTranscriptionService.settings());
    }

    private void radioReference(HttpExchange exchange) throws IOException
    {
        RadioReference radioReference = mPlaylistManager.getRadioReference();
        initializeRadioReference(radioReference);
        if("GET".equals(exchange.getRequestMethod()))
        {
            Map<String,Object> status = new LinkedHashMap<>();
            String username = mUserPreferences.getRadioReferencePreference().getUserName();
            status.put("configured", mUserPreferences.getRadioReferencePreference().hasStoredCredentials());
            status.put("userName", username != null ? username : "");
            status.put("storeCredentials", mUserPreferences.getRadioReferencePreference().isStoreCredentials());
            status.put("status", radioReference.getLoginStatus().name());
            status.put("premium", radioReference.premiumAccountProperty().get());
            status.put("expires", valueOrEmpty(radioReference.accountExpiresProperty().get()));
            status.put("aliasLists", radioReferenceAliasLists());
            status.put("preferredCountryId", mUserPreferences.getRadioReferencePreference().getPreferredCountryId());
            status.put("preferredStateId", mUserPreferences.getRadioReferencePreference().getPreferredStateId());
            status.put("preferredCountyId", mUserPreferences.getRadioReferencePreference().getPreferredCountyId());
            json(exchange, 200, status);
            return;
        }
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8),
            JsonObject.class);
        try
        {
            String action = requiredString(request, "action");
            if("credentials".equals(action))
            {
                String username = requiredString(request, "username");
                String password = requiredString(request, "password");
                boolean store = booleanValue(request, "store", true);
                try
                {
                    RadioReference.LoginStatus status = RadioReference.testConnectionWithExp(username, password);
                    if(status == RadioReference.LoginStatus.VALID_PREMIUM)
                    {
                        if(store)
                        {
                            mUserPreferences.getRadioReferencePreference().setUserName(username);
                            mUserPreferences.getRadioReferencePreference().setPassword(password);
                            mUserPreferences.getRadioReferencePreference().setStoreCredentials(true);
                        }
                        else { mUserPreferences.getRadioReferencePreference().removeStoredCredentials(); }
                        radioReference.setAuthorizationInformation(RadioReference.getAuthorizatonInformation(username, password));
                        mRadioReferenceInitialized = true;
                    }
                    String message = switch(status)
                    {
                        case VALID_PREMIUM -> "Signed in with RadioReference Premium access";
                        case EXPIRED_PREMIUM -> "The RadioReference Premium subscription is expired";
                        case INVALID_LOGIN -> "RadioReference rejected the username or password";
                        default -> "RadioReference login status is unknown";
                    };
                    json(exchange, 200, Map.of("success", status == RadioReference.LoginStatus.VALID_PREMIUM,
                        "status", status.name(), "message", message));
                }
                catch(Exception e)
                {
                    json(exchange, 200, Map.of("success", false, "status", "UNAVAILABLE",
                        "message", radioReferenceError(e)));
                }
                return;
            }
            var service = radioReference.getService();
            if("countries".equals(action))
            {
                List<Map<String,Object>> countries = new ArrayList<>();
                for(var country: service.getCountries())
                {
                    countries.add(Map.of("id", country.getCountryId(), "name", valueOrEmpty(country.getName()),
                        "code", valueOrEmpty(country.getCountryCode())));
                }
                json(exchange, 200, Map.of("countries", countries));
                return;
            }
            if("country".equals(action))
            {
                int countryId = integerValue(request, "countryId", -1);
                var info = service.getCountryInfo(countryId);
                mUserPreferences.getRadioReferencePreference().setPreferredCountryId(countryId);
                List<Map<String,Object>> states = new ArrayList<>();
                if(info.getStates() != null)
                {
                    for(var state: info.getStates())
                    { states.add(Map.of("id", state.getStateId(), "name", valueOrEmpty(state.getName()), "code", valueOrEmpty(state.getStateCode()))); }
                }
                json(exchange, 200, Map.of("name", valueOrEmpty(info.getName()), "states", states));
                return;
            }
            if("state".equals(action))
            {
                int stateId = integerValue(request, "stateId", -1);
                var info = service.getStateInfo(stateId);
                mUserPreferences.getRadioReferencePreference().setPreferredStateId(stateId);
                List<Map<String,Object>> counties = new ArrayList<>();
                if(info.getCounties() != null)
                {
                    for(var county: info.getCounties())
                    { counties.add(Map.of("id", county.getCountyId(), "name", valueOrEmpty(county.getName()))); }
                }
                json(exchange, 200, Map.of("name", valueOrEmpty(info.getName()), "counties", counties,
                    "systems", radioReferenceSystems(info.getSystems())));
                return;
            }
            if("county".equals(action))
            {
                int countyId = integerValue(request, "countyId", -1);
                var info = service.getCountyInfo(countyId);
                mUserPreferences.getRadioReferencePreference().setPreferredCountyId(countyId);
                json(exchange, 200, Map.of("name", valueOrEmpty(info.getName()),
                    "systems", radioReferenceSystems(info.getSystems())));
                return;
            }
            if("createAliasList".equals(action))
            {
                String name = requiredString(request, "name").trim();
                if(name.length() > 25) { throw new IllegalArgumentException("Alias list names are limited to 25 characters"); }
                mPlaylistManager.getAliasModel().addAliasList(name);
                mPlaylistManager.schedulePlaylistSave();
                json(exchange, 200, Map.of("aliasLists", radioReferenceAliasLists(), "selected", name));
                return;
            }
            int systemId = integerValue(request, "systemId", -1);
            io.github.dsheirer.rrapi.type.System system = radioReferenceSystem(service, systemId);
            List<io.github.dsheirer.rrapi.type.Talkgroup> groups = service.getTalkgroups(systemId);
            List<TalkgroupCategory> categories = service.getTalkgroupCategories(systemId);
            Map<Integer,String> categoryNames = new LinkedHashMap<>();
            for(TalkgroupCategory category: categories) { categoryNames.put(category.getTalkgroupCategoryId(), category.getName()); }
            RadioReferenceDecoder decoder = new RadioReferenceDecoder(mUserPreferences, service.getTypesMap(),
                service.getFlavorsMap(), service.getVoicesMap(), service.getTagsMap());
            if("preview".equals(action))
            {
                String aliasListName = request.has("aliasList") ? request.get("aliasList").getAsString() : "";
                List<Map<String,Object>> result = new ArrayList<>();
                for(var group: groups)
                {
                    Talkgroup aliasId = decoder.getTalkgroupAliasId(group, system);
                    Map<String,Object> item = new LinkedHashMap<>();
                    item.put("id", group.getTalkgroupId());
                    item.put("value", decoder.getTalkgroupValue(group, system));
                    item.put("name", valueOrEmpty(group.getAlphaTag()));
                    item.put("description", valueOrEmpty(group.getDescription()));
                    item.put("categoryId", group.getTalkgroupCategoryId());
                    item.put("category", categoryNames.getOrDefault(group.getTalkgroupCategoryId(), ""));
                    item.put("protocol", decoder.getProtocol(system).name());
                    item.put("mode", valueOrEmpty(group.getMode()));
                    item.put("encryption", TalkgroupEncryption.lookup(group.getEncryptionState()).toString());
                    item.put("fullyEncrypted", group.isFullyEncrypted());
                    item.put("imported", !aliasListName.isBlank() && radioReferenceAliasExists(aliasListName, aliasId));
                    result.add(item);
                }
                List<Map<String,Object>> categoryResult = new ArrayList<>();
                for(TalkgroupCategory category: categories)
                { categoryResult.add(Map.of("id", category.getTalkgroupCategoryId(), "name", valueOrEmpty(category.getName()))); }
                json(exchange, 200, Map.of("system", valueOrEmpty(system.getName()),
                    "protocol", decoder.getProtocol(system).name(), "supported", decoder.hasSupportedProtocol(system),
                    "categories", categoryResult, "talkgroups", result));
                return;
            }
            if("import".equals(action))
            {
                String aliasListName = requiredString(request, "aliasList");
                AliasList aliasList = mPlaylistManager.getAliasModel().getAliasList(aliasListName);
                if(aliasList == null) { throw new IllegalArgumentException("Select or create an alias list before importing"); }
                Set<Integer> selected = new HashSet<>();
                if(request.has("talkgroupIds") && request.get("talkgroupIds").isJsonArray())
                { request.getAsJsonArray("talkgroupIds").forEach(value -> selected.add(value.getAsInt())); }
                boolean encryptedMuted = booleanValue(request, "encryptedMuted", false);
                List<Alias> created = new ArrayList<>();
                for(var group: groups)
                {
                    if(!selected.isEmpty() && !selected.contains(group.getTalkgroupId())) { continue; }
                    Alias alias = decoder.createAlias(group, system, aliasListName,
                        categoryNames.get(group.getTalkgroupCategoryId()));
                    Talkgroup id = alias.getAliasIdentifiers().stream().filter(Talkgroup.class::isInstance)
                        .map(Talkgroup.class::cast).findFirst().orElse(null);
                    if(id != null && !radioReferenceAliasExists(aliasListName, id))
                    {
                        if(encryptedMuted && group.isFullyEncrypted())
                        {
                            alias.addAliasID(new io.github.dsheirer.alias.id.priority.Priority(
                                io.github.dsheirer.alias.id.priority.Priority.DO_NOT_MONITOR));
                        }
                        created.add(alias);
                    }
                }
                mPlaylistManager.getAliasModel().addAliases(created);
                mPlaylistManager.schedulePlaylistSave();
                json(exchange, 200, Map.of("system", system.getName(), "imported", created.size(),
                    "selected", selected.isEmpty() ? groups.size() : selected.size(),
                    "available", groups.size(), "aliasList", aliasListName));
                return;
            }
            json(exchange, 400, Map.of("error", "unsupported RadioReference action"));
        }
        catch(IllegalArgumentException e) { json(exchange, 400, Map.of("error", radioReferenceError(e))); }
        catch(Exception e) { json(exchange, 502, Map.of("error", radioReferenceError(e))); }
    }

    private synchronized void initializeRadioReference(RadioReference radioReference)
    {
        if(mRadioReferenceInitialized) { return; }
        mRadioReferenceInitialized = true;
        var credentials = mUserPreferences.getRadioReferencePreference().getAuthorizationInformation();
        if(credentials != null && credentials.getUserName() != null && credentials.getPassword() != null)
        { radioReference.setAuthorizationInformation(credentials); }
    }

    private List<String> radioReferenceAliasLists()
    {
        return new ArrayList<>(mPlaylistManager.getAliasModel().aliasListNames()).stream()
            .filter(name -> name != null && !name.isBlank() && !AliasModel.NO_ALIAS_LIST.equals(name)).sorted().toList();
    }

    private static List<Map<String,Object>> radioReferenceSystems(List<io.github.dsheirer.rrapi.type.System> systems)
    {
        List<Map<String,Object>> result = new ArrayList<>();
        if(systems != null)
        {
            systems.stream().sorted().forEach(system -> result.add(Map.of("id", system.getSystemId(),
                "name", valueOrEmpty(system.getName()), "city", valueOrEmpty(system.getCity()))));
        }
        return result;
    }

    private static io.github.dsheirer.rrapi.type.System radioReferenceSystem(
        io.github.dsheirer.rrapi.RadioReferenceService service, int systemId) throws Exception
    {
        if(systemId < 1) { throw new IllegalArgumentException("Select a RadioReference system"); }
        var information = service.getSystemInformation(systemId);
        var system = new io.github.dsheirer.rrapi.type.System();
        system.setSystemId(systemId);
        system.setName(information.getName());
        system.setTypeId(information.getTypeId());
        system.setFlavorId(information.getFlavorId());
        system.setVoiceId(information.getVoiceId());
        system.setCity(information.getCity());
        system.setLastUpdated(information.getLastUpdated());
        return system;
    }

    private boolean radioReferenceAliasExists(String aliasListName, Talkgroup id)
    {
        return mPlaylistManager.getAliasModel().getAliases().stream()
            .filter(existing -> aliasListName.equals(existing.getAliasListName()))
            .flatMap(existing -> existing.getAliasIdentifiers().stream())
            .filter(Talkgroup.class::isInstance).map(Talkgroup.class::cast)
            .anyMatch(existing -> existing.getProtocol() == id.getProtocol() && existing.getValue() == id.getValue());
    }

    private static String radioReferenceError(Throwable error)
    {
        Throwable cause = error;
        while(cause.getCause() != null) { cause = cause.getCause(); }
        String message = error.getMessage();
        if(message == null || message.isBlank()) { message = cause.getMessage(); }
        return message != null && !message.isBlank() ? message : "RadioReference request failed";
    }

    private static String valueOrEmpty(String value) { return value != null ? value : ""; }

    private void webSettings(HttpExchange exchange) throws IOException
    {
        if("GET".equals(exchange.getRequestMethod()))
        {
            Map<String,Object> settings = new LinkedHashMap<>();
            settings.put("running", isRunning());
            settings.put("passwordConfigured", mPassword != null && !mPassword.isBlank());
            settings.put("bind", getEffectiveBindAddress());
            settings.put("port", getEffectivePort());
            settings.put("savedBind", getSavedBindAddress());
            settings.put("savedPort", getSavedPort());
            settings.put("actualBind", mBoundAddress);
            settings.put("actualPort", mBoundPort);
            settings.put("bindOverridden", System.getProperty(BIND_KEY) != null);
            settings.put("portOverridden", System.getProperty(PORT_KEY) != null);
            settings.put("remoteAccess", mServer != null && mServer.getAddress().getAddress() != null &&
                mServer.getAddress().getAddress().isAnyLocalAddress());
            json(exchange, 200, settings);
            return;
        }
        if(!"POST".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8), JsonObject.class);
        String action = stringValue(request, "action", "password");
        if("password".equalsIgnoreCase(action))
        {
            String password;
            try
            {
                password = requiredString(request, "password").trim();
            }
            catch(IllegalArgumentException e)
            {
                json(exchange, 400, Map.of("error", e.getMessage()));
                return;
            }
            savePassword(password);
            json(exchange, 200, Map.of("saved", true, "sessionsInvalidated", true));
            return;
        }
        if("network".equalsIgnoreCase(action))
        {
            try
            {
                String bind = requiredString(request, "bind");
                int port = integerValue(request, "port", DEFAULT_PORT);
                saveNetworkSettings(bind, port);
                json(exchange, 200, Map.of("saved", true, "bind", bind.trim(), "port", port,
                    "restartRequired", true,
                    "bindOverridden", System.getProperty(BIND_KEY) != null,
                    "portOverridden", System.getProperty(PORT_KEY) != null));
            }
            catch(IllegalArgumentException e)
            {
                json(exchange, 400, Map.of("error", errorMessage(e)));
            }
            return;
        }
        if("restart".equalsIgnoreCase(action))
        {
            json(exchange, 202, Map.of("restarting", true));
            Thread.startVirtualThread(() ->
            {
                try
                {
                    Thread.sleep(250);
                    restartActive();
                }
                catch(Exception e)
                {
                    mLog.error("Unable to restart web interface", e);
                }
            });
            return;
        }
        json(exchange, 400, Map.of("error", "action must be password, network, or restart"));
    }

    private void webUsers(HttpExchange exchange) throws IOException
    {
        if("GET".equalsIgnoreCase(exchange.getRequestMethod()))
        {
            json(exchange, 200, Map.of("users", getWebUsers(), "builtInAdministrator", "admin"));
            return;
        }
        if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        JsonObject request = GSON.fromJson(new String(exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8), JsonObject.class);
        String action = stringValue(request, "action", "save");
        try
        {
            if("delete".equalsIgnoreCase(action))
            {
                deleteWebUser(requiredString(request, "id"));
                json(exchange, 200, Map.of("deleted", true));
                return;
            }
            if("save".equalsIgnoreCase(action))
            {
                WebUser saved = saveWebUser(stringValue(request, "id", ""), requiredString(request, "username"),
                    stringValue(request, "password", ""), stringValue(request, "role", "operator"),
                    booleanValue(request, "enabled", true));
                json(exchange, 200, Map.of("saved", true, "user", saved));
                return;
            }
            json(exchange, 400, Map.of("error", "action must be save or delete"));
        }
        catch(IllegalArgumentException e)
        {
            json(exchange, 400, Map.of("error", errorMessage(e)));
        }
    }

    private void applicationLog(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        String selection = queryValue(exchange, "log");
        Path root = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog().toAbsolutePath().normalize();
        Path file = "previous".equalsIgnoreCase(selection) ? previousApplicationLog(root) :
            root.resolve("sdrtrunk_app.log");
        if(file == null || !Files.isRegularFile(file))
        {
            json(exchange, 200, Map.of("available", false, "entries", List.of(), "incremental", false,
                "max_entries", LOG_ENTRY_LIMIT, "updated_at", Instant.now().toString()));
            return;
        }
        long modified = lastModified(file);
        String revision = Long.toHexString(modified) + Long.toHexString(fileSize(file));
        List<Map<String,Object>> entries = parseApplicationLog(readFileTail(file, LOG_TAIL_BYTES), revision);
        Map<String,Object> result = new LinkedHashMap<>();
        result.put("available", true);
        result.put("file_name", file.getFileName().toString());
        result.put("entries", entries);
        result.put("incremental", false);
        result.put("revision", revision);
        result.put("latest_id", entries.isEmpty() ? null : entries.getLast().get("id"));
        result.put("max_entries", LOG_ENTRY_LIMIT);
        result.put("truncated", fileSize(file) > LOG_TAIL_BYTES || entries.size() >= LOG_ENTRY_LIMIT);
        result.put("updated_at", Instant.now().toString());
        json(exchange, 200, result);
    }

    private Path previousApplicationLog(Path root) throws IOException
    {
        if(!Files.isDirectory(root)) { return null; }
        try(var paths = Files.list(root))
        {
            return paths.filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().endsWith("_sdrtrunk_app.log"))
                .max(Comparator.comparingLong(SdrTrunkWebServer::lastModified)).orElse(null);
        }
    }

    private static String readFileTail(Path file, int maximumBytes) throws IOException
    {
        long size = Files.size(file);
        int length = (int)Math.min(size, maximumBytes);
        ByteBuffer buffer = ByteBuffer.allocate(length);
        try(FileChannel channel = FileChannel.open(file, StandardOpenOption.READ))
        {
            channel.position(Math.max(0, size - length));
            while(buffer.hasRemaining() && channel.read(buffer) >= 0) {}
        }
        buffer.flip();
        String value = StandardCharsets.UTF_8.decode(buffer).toString();
        if(size > maximumBytes)
        {
            int firstLine = value.indexOf('\n');
            if(firstLine >= 0) { value = value.substring(firstLine + 1); }
        }
        return value;
    }

    private static List<Map<String,Object>> parseApplicationLog(String text, String revision)
    {
        List<Map<String,Object>> parsed = new ArrayList<>();
        Map<String,Object> current = null;
        StringBuilder details = new StringBuilder();
        for(String line: text.split("\\R"))
        {
            Matcher matcher = APPLICATION_LOG_LINE.matcher(line);
            if(matcher.matches())
            {
                if(current != null)
                {
                    if(!details.isEmpty()) { current.put("details", details.toString()); }
                    parsed.add(current);
                }
                current = new LinkedHashMap<>();
                current.put("id", revision + ":" + parsed.size());
                current.put("time", applicationLogTime(matcher.group(1)));
                current.put("thread", matcher.group(2));
                current.put("level", matcher.group(3));
                current.put("source", matcher.group(4));
                current.put("message", matcher.group(5));
                current.put("text", line);
                details = new StringBuilder();
            }
            else if(current != null)
            {
                if(!details.isEmpty()) { details.append('\n'); }
                details.append(line);
            }
        }
        if(current != null)
        {
            if(!details.isEmpty()) { current.put("details", details.toString()); }
            parsed.add(current);
        }
        return parsed.size() <= LOG_ENTRY_LIMIT ? parsed :
            new ArrayList<>(parsed.subList(parsed.size() - LOG_ENTRY_LIMIT, parsed.size()));
    }

    private static String applicationLogTime(String value)
    {
        try
        {
            return LocalDateTime.parse(value, APPLICATION_LOG_TIME).atZone(ZoneId.systemDefault()).toInstant().toString();
        }
        catch(DateTimeParseException e)
        {
            return null;
        }
    }

    private void eventLogs(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        Path root = mUserPreferences.getDirectoryPreference().getDirectoryEventLog().toAbsolutePath().normalize();
        String name = queryValue(exchange, "file");
        if(name == null || name.isBlank())
        {
            if(!Files.isDirectory(root)) { json(exchange, 200, List.of()); return; }
            try(var paths = Files.list(root))
            {
                List<Map<String,Object>> result = paths.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".log"))
                    .sorted((a,b) -> Long.compare(lastModified(b), lastModified(a))).limit(200)
                    .map(path -> Map.<String,Object>of("name", path.getFileName().toString(),
                        "size", fileSize(path), "modified", lastModified(path))).toList();
                json(exchange, 200, result);
            }
            return;
        }
        Path file = root.resolve(name).normalize();
        if(!file.startsWith(root) || !file.getParent().equals(root) || !Files.isRegularFile(file) ||
            !name.toLowerCase().endsWith(".log"))
        {
            json(exchange, 404, Map.of("error", "event log not found"));
            return;
        }
        if("true".equalsIgnoreCase(queryValue(exchange, "download")))
        {
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"" +
                file.getFileName().toString().replace("\"", "") + "\"");
            streamFile(exchange, file, "text/plain; charset=UTF-8");
            return;
        }
        json(exchange, 200, Map.of("name", file.getFileName().toString(), "size", Files.size(file),
            "modified", lastModified(file), "content", readFileTail(file, LOG_TAIL_BYTES),
            "truncated", Files.size(file) > LOG_TAIL_BYTES));
    }

    private void recordings(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        Path root = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
        if(!Files.isDirectory(root)) { json(exchange, 200, List.of()); return; }
        try(var paths = Files.list(root))
        {
            List<Map<String,Object>> result = paths.filter(Files::isRegularFile)
                .filter(path -> isAudioFile(path.getFileName().toString()))
                .sorted((a,b) -> Long.compare(lastModified(b), lastModified(a))).limit(200)
                .map(path -> Map.<String,Object>of("name", path.getFileName().toString(),
                    "size", fileSize(path), "modified", lastModified(path))).toList();
            json(exchange, 200, result);
        }
    }

    private void recordingAudio(HttpExchange exchange) throws IOException
    {
        if(!"GET".equals(exchange.getRequestMethod())) { methodNotAllowed(exchange); return; }
        String name = queryValue(exchange, "name");
        Path root = mUserPreferences.getDirectoryPreference().getDirectoryRecording().toAbsolutePath().normalize();
        Path file = name == null ? root : root.resolve(name).normalize();
        if(name == null || !file.startsWith(root) || !file.getParent().equals(root) || !Files.isRegularFile(file) ||
            !isAudioFile(name))
        {
            json(exchange, 404, Map.of("error", "recording not found"));
            return;
        }
        bytes(exchange, 200, name.toLowerCase().endsWith(".wav") ? "audio/wav" : "audio/mpeg",
            Files.readAllBytes(file));
    }

    private static boolean isAudioFile(String name)
    {
        String lower = name.toLowerCase();
        return lower.endsWith(".mp3") || lower.endsWith(".wav");
    }

    private static long lastModified(Path path) { try { return Files.getLastModifiedTime(path).toMillis(); } catch(Exception e) { return 0; } }
    private static long fileSize(Path path) { try { return Files.size(path); } catch(Exception e) { return 0; } }
    private static String queryValue(HttpExchange exchange, String key)
    {
        String query = exchange.getRequestURI().getRawQuery();
        if(query == null) { return null; }
        for(String part: query.split("&"))
        {
            String[] pair = part.split("=", 2);
            if(URLDecoder.decode(pair[0], StandardCharsets.UTF_8).equals(key))
            { return pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : ""; }
        }
        return null;
    }
    private static long longQueryValue(HttpExchange exchange, String key, long fallback)
    { try { return Long.parseLong(queryValue(exchange, key)); } catch(Exception e) { return fallback; } }

    private static void bytes(HttpExchange exchange, int status, String contentType, byte[] value) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, value.length);
        exchange.getResponseBody().write(value);
        exchange.close();
    }

    private static void streamFile(HttpExchange exchange, Path file, String contentType) throws IOException
    {
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(200, Files.size(file));
        try(var input = Files.newInputStream(file))
        {
            input.transferTo(exchange.getResponseBody());
        }
        exchange.close();
    }

    private static void methodNotAllowed(HttpExchange exchange) throws IOException
    {
        json(exchange, 405, Map.of("error", "method not allowed"));
    }

    private static void json(HttpExchange exchange, int status, Object value) throws IOException
    {
        byte[] bytes = GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void vceJson(HttpExchange exchange, int status, Object value) throws IOException
    {
        json(exchange, status, Map.of("data", value));
    }

    private static void vceError(HttpExchange exchange, int status, String code, String message) throws IOException
    {
        json(exchange, status, Map.of("error", Map.of("code", code, "message", message, "status", status)));
    }

    private static final String VCE_DEFAULT_PREFERENCES = """
        {
          "version": 12,
          "appearance": {"theme": "dark", "hue": null},
          "page_titles": {"prepend_playing_call": true},
          "playback": {"volume": 1.0, "selected_scan_list_ids": [], "target_grouping": true,
            "target_burst_limit": 4},
          "scanner": {"detail_mode": "normal"},
          "presentation": {"source_name_display": "talker_alias", "show_encryption_details": true,
            "show_control_decode_quality": true, "show_voice_decode_quality": true,
            "decode_quality_display_mode": "percentage", "live_detail_row_limit": 200,
            "show_only_active_trunked_channels": true, "live_channel_sort": "order_appeared",
            "live_row_density": "normal", "retain_last_call_on_idle_rows": false,
            "clear_voice_quality_when_idle": false},
          "tuner": {"floor_db": -140, "ceiling_db": 0, "waterfall_speed": 1.0,
            "snap_frequency": true, "smooth_fft": true, "highlight_waterfall_channels": false,
            "show_idle_channels": false, "profile": "balanced"},
          "health_alerts": {"disabled_codes": []},
          "tables": {}
        }
        """;

}
