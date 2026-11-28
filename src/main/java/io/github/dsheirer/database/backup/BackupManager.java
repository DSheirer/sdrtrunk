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
package io.github.dsheirer.database.backup;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.ICallRepository;
import io.github.dsheirer.database.call.RecordingState;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.IEventRepository;
import io.github.dsheirer.database.playlist.PlaylistItem;
import io.github.dsheirer.database.playlist.PlaylistItemType;
import io.github.dsheirer.database.playlist.PlaylistRecord;
import io.github.dsheirer.database.playlist.PlaylistRepository;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.database.DatabaseSettings;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/**
 * Database backups: scheduled and on-demand backups of the calls and events, retention of the newest backups, and
 * restore of a backup into the current database.
 *
 * A backup is a zip file with the calls and events as JSON (see {@link BackupCodec}) and a metadata entry.  The format
 * doesn't depend on the database engine, so a backup can be restored into any of the supported databases.  The
 * recording files aren't included - restored calls refer to the recording files that still exist.
 *
 * Backups and restores run one at a time on the backup thread.  A backup reads the database in a single read
 * transaction while the database thread continues to write.  A restore runs on the database thread (writes are held
 * until it's done) and first makes a 'pre-restore' backup of the current contents, so it can be undone.
 */
public class BackupManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(BackupManager.class);
    public static final int FORMAT_VERSION = 1;
    public static final String TAG_PRE_RESTORE = "pre-restore";
    static final String FILE_PREFIX = "sdrtrunk-backup-";
    static final String FILE_EXTENSION = ".zip";
    static final String ENTRY_METADATA = "metadata.json";
    static final String ENTRY_CALLS = "calls.json";
    static final String ENTRY_EVENTS = "events.json";
    static final String ENTRY_PLAYLISTS = "playlists.json";
    private static final Pattern FILE_PATTERN =
        Pattern.compile("sdrtrunk-backup-(\\d{8}-\\d{6})(?:-([a-z][a-z0-9-]*))?\\.zip");
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final long INITIAL_DELAY_SECONDS = 120;
    private static final long CHECK_INTERVAL_SECONDS = 600;
    private static final int PAGE_SIZE = 1000;

    private final DatabaseManager mDatabaseManager;
    private final DatabaseSettings mDatabaseSettings;
    private final Supplier<Path> mDirectory;
    private final ScheduledExecutorService mExecutor =
        Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk database backup"));
    private final Broadcaster<BackupInfo> mBackupBroadcaster = new Broadcaster<>();
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * Constructs an instance
     * @param databaseManager for the current database
     * @param settings for the backup schedule, retention and directory
     */
    public BackupManager(DatabaseManager databaseManager, Settings settings)
    {
        mDatabaseManager = databaseManager;
        mDatabaseSettings = settings.getDatabaseSettings();
        mDirectory = () -> settings.getDirectorySettings().getDirectoryDatabaseBackup();
    }

    /**
     * Starts the automatic backup schedule.  The schedule is checked two minutes after startup and then every 10
     * minutes, and a backup is made when the newest backup is older than the backup interval.
     */
    public void start()
    {
        if(mScheduledFuture == null)
        {
            mScheduledFuture = mExecutor.scheduleWithFixedDelay(this::checkSchedule, INITIAL_DELAY_SECONDS,
                CHECK_INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops the schedule.  A backup in progress is abandoned and its partial file removed.
     */
    public void stop()
    {
        if(mScheduledFuture != null)
        {
            mScheduledFuture.cancel(false);
            mScheduledFuture = null;
        }

        mExecutor.shutdownNow();
    }

    /**
     * Directory that holds the backup files
     */
    public Path getBackupDirectory()
    {
        return mDirectory.get();
    }

    /**
     * Registers the listener to be notified of each new backup (scheduled, manual or pre-restore).  Notified on the
     * backup thread.
     */
    public void addBackupListener(Listener<BackupInfo> listener)
    {
        mBackupBroadcaster.addListener(listener);
    }

    public void removeBackupListener(Listener<BackupInfo> listener)
    {
        mBackupBroadcaster.removeListener(listener);
    }

    private void checkSchedule()
    {
        try
        {
            BackupInterval interval = mDatabaseSettings.getBackupInterval();
            List<BackupInfo> backups = list(getBackupDirectory());
            long last = backups.isEmpty() ? 0 : backups.getFirst().created();

            if(isDue(last, interval, System.currentTimeMillis()) && mDatabaseManager.isConnected())
            {
                LOGGER.info("Starting scheduled database backup (" + interval + ")");
                backup(null);
            }
        }
        catch(Exception e)
        {
            LOGGER.error("Scheduled database backup failed - " + e.getMessage(), e);
        }
    }

    /**
     * Indicates if an automatic backup is due.
     * @param lastBackup time of the newest backup, or 0 if there are no backups
     * @param interval between automatic backups
     * @param now current time
     */
    static boolean isDue(long lastBackup, BackupInterval interval, long now)
    {
        return interval.isEnabled() && (lastBackup <= 0 || now - lastBackup >= interval.getMilliseconds());
    }

    /**
     * Backs up the current database now and then deletes the oldest backups beyond the retention count.
     */
    public CompletableFuture<BackupInfo> backupNow()
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                return backup(null);
            }
            catch(Exception e)
            {
                throw new IllegalStateException(e.getMessage(), e);
            }
        }, mExecutor);
    }

    /**
     * Makes a backup and applies the retention count.  Backup thread only.
     */
    private BackupInfo backup(String tag) throws IOException, SQLException
    {
        CallDatabase database = mDatabaseManager.getDatabase();

        if(database == null)
        {
            throw new IllegalStateException("The call database is not available");
        }

        BackupInfo backup = writeBackup(database, getBackupDirectory(), tag, System.currentTimeMillis());
        LOGGER.info("Database backup created [" + backup.path() + "] with " + backup.metadata().calls() +
            " calls, " + backup.metadata().events() + " events and " + backup.metadata().playlists() + " playlists");

        if(tag == null)
        {
            for(Path deleted: applyRetention(getBackupDirectory(), mDatabaseSettings.getBackupRetain()))
            {
                LOGGER.info("Deleted old database backup [" + deleted + "]");
            }
        }

        mBackupBroadcaster.broadcast(backup);
        return backup;
    }

    /**
     * Lists the backups, newest first.  Runs on the backup thread.
     */
    public CompletableFuture<List<BackupInfo>> listBackups()
    {
        return CompletableFuture.supplyAsync(() -> list(getBackupDirectory()), mExecutor);
    }

    /**
     * Deletes the backup file.  Runs on the backup thread.
     */
    public CompletableFuture<Void> delete(BackupInfo backup)
    {
        return CompletableFuture.runAsync(() -> {
            try
            {
                Files.deleteIfExists(backup.path());
                LOGGER.info("Deleted database backup [" + backup.path() + "]");
            }
            catch(IOException ioe)
            {
                throw new IllegalStateException(ioe.getMessage(), ioe);
            }
        }, mExecutor);
    }

    /**
     * Replaces the calls and events in the current database with the contents of the backup.  A pre-restore backup
     * of the current contents is made first (when the database isn't empty).
     * @param backup to restore
     * @param progress receives status text (on the backup or database thread)
     */
    public CompletableFuture<RestoreResult> restore(BackupInfo backup, Consumer<String> progress)
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                BackupInfo.Metadata metadata = readMetadata(backup.path());
                checkCompatible(metadata);
                CallDatabase database = mDatabaseManager.getDatabase();

                if(database == null)
                {
                    throw new IllegalStateException("The call database is not available");
                }

                BackupInfo preRestore = null;

                if(database.getCallCount() > 0 || database.getEventCount() > 0 || database.getPlaylistCount() > 0)
                {
                    progress.accept("Backing up the current database before the restore");
                    preRestore = backup(TAG_PRE_RESTORE);
                }

                RestoreResult result = mDatabaseManager.restore(backup.path(), progress).get();
                LOGGER.info("Restored database backup [" + backup.path() + "] - " + result.calls() + " calls, " +
                    result.events() + " events, " + result.missingRecordings() + " missing recordings" +
                    (result.playlists() >= 0 ? ", " + result.playlists() + " playlists" : ""));
                return result.withPreRestore(preRestore);
            }
            catch(Exception e)
            {
                Throwable cause = e.getCause() != null && e instanceof java.util.concurrent.ExecutionException ?
                    e.getCause() : e;
                throw new IllegalStateException(cause.getMessage(), cause);
            }
        }, mExecutor);
    }

    /**
     * Result of a restore
     * @param calls restored
     * @param events restored
     * @param missingRecordings count of restored calls whose recording file no longer exists
     * @param playlists restored, or -1 if the backup didn't have playlists and the playlists weren't changed
     * @param preRestore backup of the database contents before the restore, or null if the database was empty
     */
    public record RestoreResult(long calls, long events, long missingRecordings, long playlists,
                                BackupInfo preRestore)
    {
        RestoreResult withPreRestore(BackupInfo backup)
        {
            return new RestoreResult(calls, events, missingRecordings, playlists, backup);
        }
    }

    /**
     * Throws if the backup can't be restored by this version of sdrtrunk.
     */
    static void checkCompatible(BackupInfo.Metadata metadata) throws IOException
    {
        if(metadata == null)
        {
            throw new IOException("The file is not a readable sdrtrunk database backup");
        }

        if(metadata.format() > FORMAT_VERSION || metadata.schemaVersion() > CallDatabase.SCHEMA_VERSION)
        {
            throw new IOException("The backup was made by a newer version of sdrtrunk (backup format " +
                metadata.format() + ", schema version " + metadata.schemaVersion() + ")");
        }
    }

    /**
     * Writes a backup of the calls and events in the database to a new file in the directory.  The file is written
     * to a temporary name and renamed when complete, so an interrupted backup never leaves a partial backup file.
     * @param database to back up
     * @param directory for the backup file
     * @param tag for the file name, or null
     * @param now backup time
     * @return the new backup
     */
    public static BackupInfo writeBackup(CallDatabase database, Path directory, String tag, long now)
        throws IOException, SQLException
    {
        Files.createDirectories(directory);
        long created = now - (now % 1000);
        Path target = directory.resolve(getFileName(created, tag));

        //Unique name when two backups are made in the same second
        while(Files.exists(target))
        {
            created += 1000;
            target = directory.resolve(getFileName(created, tag));
        }

        Path temp = directory.resolve(target.getFileName() + ".part");
        long calls = 0;
        long events = 0;
        long playlists = 0;

        try(ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(temp)));
            Connection connection = database.getConnection())
        {
            boolean autoCommit = connection.getAutoCommit();
            int isolation = connection.getTransactionIsolation();

            try
            {
                //One read transaction, so that the calls and events are a consistent snapshot
                connection.setAutoCommit(false);
                setIsolation(connection, Connection.TRANSACTION_REPEATABLE_READ);

                ICallRepository callRepository = database.getCallRepository();
                JsonWriter writer = startEntry(zip, ENTRY_CALLS);
                writer.beginArray();
                List<CallRecord> callPage = callRepository.readPage(connection, 0, PAGE_SIZE);

                while(!callPage.isEmpty())
                {
                    for(CallRecord call: callPage)
                    {
                        BackupCodec.write(writer, call);
                    }

                    calls += callPage.size();
                    callPage = callRepository.readPage(connection, callPage.getLast().getId(), PAGE_SIZE);
                }

                writer.endArray();
                writer.flush();
                zip.closeEntry();

                IEventRepository eventRepository = database.getEventRepository();
                writer = startEntry(zip, ENTRY_EVENTS);
                writer.beginArray();
                List<EventRecord> eventPage = eventRepository.readPage(connection, 0, PAGE_SIZE);

                while(!eventPage.isEmpty())
                {
                    for(EventRecord event: eventPage)
                    {
                        BackupCodec.write(writer, event);
                    }

                    events += eventPage.size();
                    eventPage = eventRepository.readPage(connection, eventPage.getLast().getId(), PAGE_SIZE);
                }

                writer.endArray();
                writer.flush();
                zip.closeEntry();

                PlaylistRepository playlistRepository = database.getPlaylistRepository();
                writer = startEntry(zip, ENTRY_PLAYLISTS);
                writer.beginArray();

                for(PlaylistRecord playlist: playlistRepository.list(connection))
                {
                    writer.beginObject();
                    writer.name("name").value(playlist.name());
                    writer.name("version").value(playlist.version());
                    writer.name("created_time").value(playlist.created());
                    writer.name("modified_time").value(playlist.modified());
                    writer.name("items").beginArray();

                    for(PlaylistItem item: playlistRepository.readItems(connection, playlist.id()))
                    {
                        writer.beginObject();
                        writer.name("item_type").value(item.type().name());
                        writer.name("name").value(item.name());
                        writer.name("config").value(item.config());
                        writer.endObject();
                    }

                    writer.endArray();
                    writer.endObject();
                    playlists++;
                }

                writer.endArray();
                writer.flush();
                zip.closeEntry();
                connection.rollback();
            }
            finally
            {
                setIsolation(connection, isolation);
                connection.setAutoCommit(autoCommit);
            }

            BackupInfo.Metadata metadata = new BackupInfo.Metadata(FORMAT_VERSION, CallDatabase.SCHEMA_VERSION,
                created, database.getConfiguration().getDescription(), calls, events, playlists);
            JsonWriter writer = startEntry(zip, ENTRY_METADATA);
            writer.setIndent("  ");
            writer.beginObject();
            writer.name("format").value(metadata.format());
            writer.name("schema_version").value(metadata.schemaVersion());
            writer.name("created").value(metadata.created());
            writer.name("source").value(metadata.source());
            writer.name("calls").value(metadata.calls());
            writer.name("events").value(metadata.events());
            writer.name("playlists").value(metadata.playlists());
            writer.endObject();
            writer.flush();
            zip.closeEntry();
        }
        catch(IOException | SQLException | RuntimeException e)
        {
            Files.deleteIfExists(temp);
            throw e;
        }

        try
        {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
        }
        catch(AtomicMoveNotSupportedException e)
        {
            Files.move(temp, target);
        }

        return read(target);
    }

    private static void setIsolation(Connection connection, int isolation)
    {
        try
        {
            connection.setTransactionIsolation(isolation);
        }
        catch(SQLException se)
        {
            LOGGER.debug("Transaction isolation level not supported - " + se.getMessage());
        }
    }

    /**
     * Starts a zip entry and returns a JSON writer for it.  The writer must be flushed, not closed, since closing it
     * would close the zip stream.
     */
    private static JsonWriter startEntry(ZipOutputStream zip, String name) throws IOException
    {
        zip.putNextEntry(new ZipEntry(name));
        OutputStream entry = new FilterOutputStream(zip)
        {
            @Override
            public void write(byte[] b, int off, int len) throws IOException
            {
                out.write(b, off, len);
            }

            @Override
            public void close() throws IOException
            {
                flush();
            }
        };
        return new JsonWriter(new OutputStreamWriter(entry, StandardCharsets.UTF_8));
    }

    /**
     * Backup file name for the time and tag
     */
    static String getFileName(long created, String tag)
    {
        String time = FILE_TIME.format(LocalDateTime.ofInstant(Instant.ofEpochMilli(created), ZoneId.systemDefault()));
        return FILE_PREFIX + time + (tag != null ? "-" + tag : "") + FILE_EXTENSION;
    }

    /**
     * Replaces the calls and events in the database with the contents of the backup, in a single transaction, so
     * that a failed restore leaves the database unchanged.  Calls that were in progress when the backup was made are
     * marked interrupted, and calls whose recording file no longer exists are marked as having a deleted recording.
     * Invoke on the database thread only.
     * @param database to restore into
     * @param backup file
     * @param progress receives status text
     */
    public static RestoreResult restore(CallDatabase database, Path backup, Consumer<String> progress)
        throws IOException, SQLException
    {
        BackupInfo.Metadata metadata = readMetadata(backup);
        checkCompatible(metadata);
        long calls = 0;
        long events = 0;
        long missing = 0;
        long playlists = -1;

        try(ZipFile zip = new ZipFile(backup.toFile()); Connection connection = database.getConnection())
        {
            ZipEntry callsEntry = zip.getEntry(ENTRY_CALLS);
            ZipEntry eventsEntry = zip.getEntry(ENTRY_EVENTS);

            if(callsEntry == null || eventsEntry == null)
            {
                throw new IOException("The backup file is incomplete");
            }

            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            try
            {
                ICallRepository callRepository = database.getCallRepository();
                IEventRepository eventRepository = database.getEventRepository();
                progress.accept("Removing the current calls and events");
                eventRepository.deleteAll(connection);
                callRepository.deleteAll(connection);

                try(InputStream in = zip.getInputStream(callsEntry);
                    JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                {
                    List<CallRecord> batch = new ArrayList<>();
                    reader.beginArray();

                    while(reader.hasNext())
                    {
                        CallRecord call = BackupCodec.readCall(reader);

                        if(clearMissingRecording(call))
                        {
                            missing++;
                        }

                        batch.add(call);

                        if(batch.size() >= PAGE_SIZE)
                        {
                            callRepository.insertBatch(connection, batch);
                            calls += batch.size();
                            batch.clear();
                            progress.accept("Restoring calls: " + calls + " of " + metadata.calls());
                        }
                    }

                    reader.endArray();
                    callRepository.insertBatch(connection, batch);
                    calls += batch.size();
                }

                try(InputStream in = zip.getInputStream(eventsEntry);
                    JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                {
                    List<EventRecord> batch = new ArrayList<>();
                    reader.beginArray();

                    while(reader.hasNext())
                    {
                        batch.add(BackupCodec.readEvent(reader));

                        if(batch.size() >= PAGE_SIZE)
                        {
                            eventRepository.insertBatch(connection, batch);
                            events += batch.size();
                            batch.clear();
                            progress.accept("Restoring events: " + events + " of " + metadata.events());
                        }
                    }

                    reader.endArray();
                    eventRepository.insertBatch(connection, batch);
                    events += batch.size();
                }

                ZipEntry playlistsEntry = zip.getEntry(ENTRY_PLAYLISTS);

                if(playlistsEntry != null)
                {
                    PlaylistRepository playlistRepository = database.getPlaylistRepository();
                    progress.accept("Restoring playlists");
                    playlistRepository.deleteAll(connection);
                    playlists = 0;

                    try(InputStream in = zip.getInputStream(playlistsEntry);
                        JsonReader reader = new JsonReader(new InputStreamReader(in, StandardCharsets.UTF_8)))
                    {
                        reader.beginArray();

                        while(reader.hasNext())
                        {
                            restorePlaylist(reader, playlistRepository, connection);
                            playlists++;
                        }

                        reader.endArray();
                    }
                }

                callRepository.markInterrupted(connection);
                progress.accept("Committing the restored database");
                connection.commit();
            }
            catch(IOException | SQLException | RuntimeException e)
            {
                connection.rollback();
                throw e;
            }
            finally
            {
                connection.setAutoCommit(autoCommit);
            }
        }

        return new RestoreResult(calls, events, missing, playlists, null);
    }

    /**
     * Reads one playlist object of the playlists entry and stores it with its items.
     */
    private static void restorePlaylist(JsonReader reader, PlaylistRepository repository, Connection connection)
        throws IOException, SQLException
    {
        String name = null;
        int version = 0;
        long created = 0;
        long modified = 0;
        List<PlaylistItem> items = new ArrayList<>();
        reader.beginObject();

        while(reader.hasNext())
        {
            switch(reader.nextName())
            {
                case "name" -> name = reader.nextString();
                case "version" -> version = reader.nextInt();
                case "created_time" -> created = reader.nextLong();
                case "modified_time" -> modified = reader.nextLong();
                case "items" -> {
                    reader.beginArray();

                    while(reader.hasNext())
                    {
                        PlaylistItem item = readPlaylistItem(reader);

                        if(item != null)
                        {
                            items.add(item);
                        }
                    }

                    reader.endArray();
                }
                default -> reader.skipValue();
            }
        }

        reader.endObject();

        if(name == null)
        {
            throw new IOException("The backup has a playlist without a name");
        }

        PlaylistRecord playlist = repository.insert(connection, name, version, created, modified);
        List<PlaylistItem> stored = new ArrayList<>();

        for(PlaylistItem item: items)
        {
            stored.add(item.withPlaylistId(playlist.id()));
        }

        repository.insertBatch(connection, stored);
    }

    /**
     * Reads one playlist item object, or returns null for an item type that this version doesn't know.
     */
    private static PlaylistItem readPlaylistItem(JsonReader reader) throws IOException
    {
        String type = null;
        String name = null;
        String config = null;
        reader.beginObject();

        while(reader.hasNext())
        {
            switch(reader.nextName())
            {
                case "item_type" -> type = reader.nextString();
                case "name" -> {
                    if(reader.peek() == JsonToken.NULL)
                    {
                        reader.nextNull();
                    }
                    else
                    {
                        name = reader.nextString();
                    }
                }
                case "config" -> config = reader.nextString();
                default -> reader.skipValue();
            }
        }

        reader.endObject();
        PlaylistItemType itemType = PlaylistItemType.fromName(type);
        return itemType != null && config != null ? new PlaylistItem(0, 0, itemType, name, config) : null;
    }

    /**
     * Marks the recording of the call deleted when the recording file no longer exists (e.g. removed by an age-off
     * rule after the backup was made).
     * @return true if the recording was missing
     */
    static boolean clearMissingRecording(CallRecord call)
    {
        if(call.getFilePath() == null || (call.getRecordingState() != RecordingState.RECORDED &&
            call.getRecordingState() != RecordingState.RECORDING))
        {
            return false;
        }

        try
        {
            if(Files.exists(Path.of(call.getFilePath())))
            {
                return false;
            }
        }
        catch(RuntimeException re)
        {
            //Invalid path - treat as missing
        }

        call.setRecordingState(RecordingState.DELETED);
        call.setFilePath(null);
        call.setFileSize(0);
        return true;
    }

    /**
     * Backups in the directory, newest first.  Files that match the backup file name but can't be read are included
     * with null metadata.
     */
    public static List<BackupInfo> list(Path directory)
    {
        List<BackupInfo> backups = new ArrayList<>();

        if(!Files.isDirectory(directory))
        {
            return backups;
        }

        try(Stream<Path> files = Files.list(directory))
        {
            for(Path file: files.toList())
            {
                BackupInfo backup = read(file);

                if(backup != null)
                {
                    backups.add(backup);
                }
            }
        }
        catch(IOException ioe)
        {
            LOGGER.warn("Unable to list database backups in [" + directory + "] - " + ioe.getMessage());
        }

        backups.sort(Comparator.comparingLong(BackupInfo::created).reversed()
            .thenComparing(backup -> backup.path().getFileName().toString(), Comparator.reverseOrder()));
        return backups;
    }

    /**
     * Backup details for the file, or null if the file name isn't a backup file name.
     */
    static BackupInfo read(Path file)
    {
        Matcher matcher = FILE_PATTERN.matcher(file.getFileName().toString());

        if(!matcher.matches() || !Files.isRegularFile(file))
        {
            return null;
        }

        long created;

        try
        {
            created = LocalDateTime.parse(matcher.group(1), FILE_TIME).atZone(ZoneId.systemDefault()).toInstant()
                .toEpochMilli();
        }
        catch(DateTimeParseException e)
        {
            return null;
        }

        long size = 0;

        try
        {
            size = Files.size(file);
        }
        catch(IOException ioe)
        {
            //Leave as 0
        }

        BackupInfo.Metadata metadata = null;

        try
        {
            metadata = readMetadata(file);
        }
        catch(IOException | RuntimeException e)
        {
            LOGGER.warn("Unable to read database backup [" + file + "] - " + e.getMessage());
        }

        return new BackupInfo(file, created, size, matcher.group(2), metadata);
    }

    /**
     * Reads the metadata entry of a backup file
     */
    static BackupInfo.Metadata readMetadata(Path file) throws IOException
    {
        try(ZipFile zip = new ZipFile(file.toFile()))
        {
            ZipEntry entry = zip.getEntry(ENTRY_METADATA);

            if(entry == null)
            {
                return null;
            }

            try(InputStream in = zip.getInputStream(entry);
                InputStreamReader reader = new InputStreamReader(in, StandardCharsets.UTF_8))
            {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                return new BackupInfo.Metadata(json.get("format").getAsInt(), json.get("schema_version").getAsInt(),
                    json.get("created").getAsLong(), json.has("source") ? json.get("source").getAsString() : "",
                    json.get("calls").getAsLong(), json.get("events").getAsLong(),
                    json.has("playlists") ? json.get("playlists").getAsLong() : -1);
            }
        }
    }

    /**
     * Deletes the oldest backups so that no more than the retention count remain.
     * @return deleted files
     */
    public static List<Path> applyRetention(Path directory, int retain)
    {
        List<Path> deleted = new ArrayList<>();
        List<BackupInfo> backups = list(directory);

        for(int x = Math.max(1, retain); x < backups.size(); x++)
        {
            Path path = backups.get(x).path();

            try
            {
                Files.deleteIfExists(path);
                deleted.add(path);
            }
            catch(IOException ioe)
            {
                LOGGER.warn("Unable to delete old database backup [" + path + "] - " + ioe.getMessage());
            }
        }

        return deleted;
    }
}
