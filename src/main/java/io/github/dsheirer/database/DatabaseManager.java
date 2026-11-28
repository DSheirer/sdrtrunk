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
package io.github.dsheirer.database;

import io.github.dsheirer.database.ageoff.AgeOffManager;
import io.github.dsheirer.database.backup.BackupManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.ICallRepository;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.database.streaming.StreamActivityRecord;
import io.github.dsheirer.database.streaming.StreamActivityRepository;
import io.github.dsheirer.database.streaming.StreamActivitySearchCriteria;
import io.github.dsheirer.database.event.EventRecord;
import io.github.dsheirer.database.event.EventSearchCriteria;
import io.github.dsheirer.database.event.IEventRepository;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.sample.Broadcaster;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.controller.NamingThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the call database: opens the selected database at startup, writes call and decode event records in the
 * background, runs the age-off rules and switches between databases (with optional transfer of the existing calls and
 * events).
 *
 * All writes, the age-off rules and database switches run on a single database thread, so they never overlap.
 * Searches run on a separate thread so that a long write doesn't block the user interface.
 *
 * Call recording doesn't depend on the database: when the database is unavailable, call and event updates are held in
 * memory (newest update per call or event) and written when the database becomes available again.
 */
public class DatabaseManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static final long FLUSH_INTERVAL_MS = 250;
    private static final long RETRY_INTERVAL_MS = 30_000;
    private static final int MAX_PENDING_CALLS = 20_000;
    private static final int MAX_PENDING_EVENTS = 50_000;
    private static final int MAX_PENDING_STREAM_ACTIVITY = 50_000;
    private static final int TRANSFER_PAGE_SIZE = 1000;

    private final Settings mSettings;
    private final ScheduledExecutorService mDatabaseExecutor =
        Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk call database"));
    private final ExecutorService mQueryExecutor =
        Executors.newSingleThreadExecutor(new NamingThreadFactory("sdrtrunk call database query"));
    private final Broadcaster<DatabaseEvent> mEventBroadcaster = new Broadcaster<>();
    private final Broadcaster<CallRecord> mSavedCallBroadcaster = new Broadcaster<>();
    private final Broadcaster<EventRecord> mSavedEventBroadcaster = new Broadcaster<>();
    private final Map<RowHandle,CallRecord> mPendingCalls = new LinkedHashMap<>();
    private final Map<RowHandle,EventRecord> mPendingEvents = new LinkedHashMap<>();
    private final Map<RowHandle,StreamActivityRecord> mPendingStreamActivity = new LinkedHashMap<>();
    private final Broadcaster<StreamActivityRecord> mSavedStreamActivityBroadcaster = new Broadcaster<>();
    private final AgeOffManager mAgeOffManager;
    private final BackupManager mBackupManager;
    private volatile CallDatabase mDatabase;
    private volatile String mErrorMessage;
    private volatile int mGeneration = 1;
    private volatile int mPlaylistRestoreCount;
    private long mNextRetry;
    private ScheduledFuture<?> mFlushFuture;

    /**
     * Constructs an instance
     * @param settings for the database selection and age-off rules
     */
    public DatabaseManager(Settings settings)
    {
        mSettings = settings;
        mAgeOffManager = new AgeOffManager(this, settings);
        mBackupManager = new BackupManager(this, settings);
    }

    /**
     * Opens the selected database, installs or upgrades the sdrtrunk schema and starts the background writer.
     * Blocks until the database is open (or failed to open) so that calls from the first channels are stored.
     */
    public void start()
    {
        try
        {
            mDatabaseExecutor.submit(this::openConfiguredDatabase).get(30, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            LOGGER.error("Timeout or error while opening the call database", e);
        }

        mFlushFuture = mDatabaseExecutor.scheduleWithFixedDelay(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS,
            TimeUnit.MILLISECONDS);
        mAgeOffManager.start(mDatabaseExecutor);
        mBackupManager.start();
    }

    /**
     * Writes any remaining call updates and closes the database.
     */
    public void stop()
    {
        mAgeOffManager.stop();
        mBackupManager.stop();

        if(mFlushFuture != null)
        {
            mFlushFuture.cancel(false);
        }

        try
        {
            mDatabaseExecutor.submit(() -> {
                flush();
                closeDatabase();
            }).get(15, TimeUnit.SECONDS);
        }
        catch(Exception e)
        {
            LOGGER.error("Error while closing the call database", e);
        }

        mDatabaseExecutor.shutdownNow();
        mQueryExecutor.shutdownNow();
    }

    /**
     * Directory for the built-in database files
     */
    public Path getDatabaseDirectory()
    {
        return mSettings.getDirectorySettings().getDirectoryDatabase();
    }

    /**
     * Opens the database from the user preferences.  Database thread only.
     */
    private void openConfiguredDatabase()
    {
        DatabaseConfiguration configuration = mSettings.getDatabaseSettings().getDatabaseConfiguration();

        try
        {
            CallDatabase database = new CallDatabase(configuration, getDatabaseDirectory());

            try
            {
                database.install();

                try(Connection connection = database.getConnection())
                {
                    int interrupted = database.getCallRepository().markInterrupted(connection);

                    if(interrupted > 0)
                    {
                        LOGGER.info("Marked " + interrupted + " calls from the previous session as interrupted");
                    }
                }
            }
            catch(SQLException se)
            {
                database.close();
                throw se;
            }

            mDatabase = database;
            mErrorMessage = null;
            LOGGER.info("Call database opened: " + configuration.getDescription());
            broadcast(DatabaseEvent.Type.CONNECTED);
        }
        catch(SQLException se)
        {
            mErrorMessage = se.getMessage();
            mNextRetry = System.currentTimeMillis() + RETRY_INTERVAL_MS;
            LOGGER.error("Unable to open the call database [" + configuration.getDescription() + "] - " +
                se.getMessage());
            broadcast(DatabaseEvent.Type.ERROR);
        }
    }

    private void closeDatabase()
    {
        CallDatabase database = mDatabase;
        mDatabase = null;

        if(database != null)
        {
            database.close();
        }
    }

    /**
     * Currently open database, or null if the database isn't available.
     */
    public CallDatabase getDatabase()
    {
        return mDatabase;
    }

    /**
     * Current database configuration (from the preferences).
     */
    public DatabaseConfiguration getConfiguration()
    {
        CallDatabase database = mDatabase;
        return database != null ? database.getConfiguration() :
            mSettings.getDatabaseSettings().getDatabaseConfiguration();
    }

    /**
     * Indicates if the database is open
     */
    public boolean isConnected()
    {
        return mDatabase != null;
    }

    /**
     * Description of the last error opening the database, or null.
     */
    public String getErrorMessage()
    {
        return mErrorMessage;
    }

    /**
     * Creates a handle that tracks the database row of one call or event across updates.
     */
    public RowHandle createHandle()
    {
        return new RowHandle();
    }

    /**
     * Queues a snapshot of the call for insertion (first time) or update.  Only the newest snapshot for each call is
     * written, so this can be invoked as often as the call changes.
     * @param handle for the call
     * @param snapshot of the call that is not modified after this call
     */
    public void save(RowHandle handle, CallRecord snapshot)
    {
        synchronized(mPendingCalls)
        {
            mPendingCalls.put(handle, snapshot);

            //Bound memory use if the database is unavailable for a long time.  Drop the oldest completed calls first.
            if(mPendingCalls.size() > MAX_PENDING_CALLS)
            {
                Iterator<Map.Entry<RowHandle,CallRecord>> it = mPendingCalls.entrySet().iterator();

                while(it.hasNext() && mPendingCalls.size() > MAX_PENDING_CALLS)
                {
                    if(!it.next().getValue().isActive())
                    {
                        it.remove();
                    }
                }

                LOGGER.warn("Call database is unavailable - discarding the oldest pending call updates");
            }
        }
    }

    /**
     * Queues a snapshot of the decode event for insertion (first time) or update.  Only the newest snapshot for each
     * event is written, so this can be invoked as often as the event changes.
     * @param handle for the event
     * @param snapshot of the event that is not modified after this call
     */
    public void save(RowHandle handle, EventRecord snapshot)
    {
        synchronized(mPendingEvents)
        {
            mPendingEvents.put(handle, snapshot);

            //Bound memory use if the database is unavailable for a long time.  Drop the oldest events first.
            if(mPendingEvents.size() > MAX_PENDING_EVENTS)
            {
                Iterator<RowHandle> it = mPendingEvents.keySet().iterator();

                while(it.hasNext() && mPendingEvents.size() > MAX_PENDING_EVENTS)
                {
                    it.next();
                    it.remove();
                }

                LOGGER.warn("Call database is unavailable - discarding the oldest pending event updates");
            }
        }
    }

    /**
     * Queues a snapshot of the stream activity row for insertion (first time) or update.  Only the newest snapshot
     * for each row is written.
     * @param handle for the row
     * @param snapshot of the row that is not modified after this call
     */
    public void save(RowHandle handle, StreamActivityRecord snapshot)
    {
        synchronized(mPendingStreamActivity)
        {
            mPendingStreamActivity.put(handle, snapshot);

            //Bound memory use if the database is unavailable for a long time.  Drop the oldest rows first.
            if(mPendingStreamActivity.size() > MAX_PENDING_STREAM_ACTIVITY)
            {
                Iterator<RowHandle> it = mPendingStreamActivity.keySet().iterator();

                while(it.hasNext() && mPendingStreamActivity.size() > MAX_PENDING_STREAM_ACTIVITY)
                {
                    it.next();
                    it.remove();
                }

                LOGGER.warn("Call database is unavailable - discarding the oldest pending stream activity updates");
            }
        }
    }

    /**
     * Writes the pending call and event updates.  Database thread only.
     */
    private void flush()
    {
        CallDatabase database = mDatabase;

        if(database == null)
        {
            //Retry opening the database periodically
            if(mErrorMessage != null && System.currentTimeMillis() > mNextRetry)
            {
                openConfiguredDatabase();
                database = mDatabase;
            }

            if(database == null)
            {
                return;
            }
        }

        ICallRepository callRepository = database.getCallRepository();
        IEventRepository eventRepository = database.getEventRepository();

        StreamActivityRepository streamRepository = database.getStreamActivityRepository();

        if(flush(database, mPendingCalls, callRepository::insert, callRepository::update, mSavedCallBroadcaster,
            false) && flush(database, mPendingEvents, eventRepository::insert, eventRepository::update,
            mSavedEventBroadcaster, true))
        {
            flush(database, mPendingStreamActivity, streamRepository::insert, streamRepository::update,
                mSavedStreamActivityBroadcaster, true);
        }
    }

    /**
     * Inserts a row and returns its generated id
     */
    private interface IRowInsert<R>
    {
        long insert(Connection connection, R record) throws SQLException;
    }

    /**
     * Updates a row and indicates if the row exists
     */
    private interface IRowUpdate<R>
    {
        boolean update(Connection connection, R record) throws SQLException;
    }

    /**
     * Writes the pending updates of one table.  Database thread only.
     * @param database to write to
     * @param pending updates by row handle
     * @param inserter for new rows
     * @param updater for existing rows
     * @param saved broadcaster for the written records
     * @param transaction to write the whole batch in one transaction (faster for many small rows)
     * @return true if the updates were written, false on error
     */
    private <R extends DatabaseRecord> boolean flush(CallDatabase database, Map<RowHandle,R> pending,
                                                       IRowInsert<R> inserter, IRowUpdate<R> updater,
                                                       Broadcaster<R> saved,
                                                       boolean transaction)
    {
        List<Map.Entry<RowHandle,R>> batch;

        synchronized(pending)
        {
            if(pending.isEmpty())
            {
                return true;
            }

            batch = new ArrayList<>(pending.entrySet());
            pending.clear();
        }

        int written = 0;
        List<R> writtenRecords = new ArrayList<>();
        List<RowHandle> inserted = new ArrayList<>();

        try(Connection connection = database.getConnection())
        {
            boolean autoCommit = connection.getAutoCommit();

            if(transaction)
            {
                connection.setAutoCommit(false);
            }

            try
            {
                for(Map.Entry<RowHandle,R> entry: batch)
                {
                    RowHandle handle = entry.getKey();
                    R record = entry.getValue();

                    if(handle.mGeneration == mGeneration && handle.mId > 0)
                    {
                        record.setId(handle.mId);

                        if(updater.update(connection, record))
                        {
                            written++;
                            writtenRecords.add(record);
                            continue;
                        }
                    }

                    //New row, a row that started before a database switch, or a row removed by an age-off rule.
                    record.setId(0);
                    handle.mId = inserter.insert(connection, record);
                    handle.mGeneration = mGeneration;
                    inserted.add(handle);
                    written++;
                    writtenRecords.add(record);
                }

                if(transaction)
                {
                    connection.commit();
                }
            }
            catch(SQLException se)
            {
                if(transaction)
                {
                    //Nothing in the batch was written
                    rollback(connection);
                    written = 0;
                    writtenRecords.clear();

                    for(RowHandle handle: inserted)
                    {
                        handle.mId = 0;
                    }
                }

                throw se;
            }
            finally
            {
                if(transaction)
                {
                    restoreAutoCommit(connection, autoCommit);
                }
            }

            for(R record: writtenRecords)
            {
                saved.broadcast(record);
            }

            return true;
        }
        catch(SQLException se)
        {
            LOGGER.error("Error writing updates to the database - " + se.getMessage());

            for(R record: writtenRecords)
            {
                saved.broadcast(record);
            }

            //Put back the updates that weren't written, unless a newer update arrived in the meantime
            synchronized(pending)
            {
                Map<RowHandle,R> newer = new LinkedHashMap<>(pending);
                pending.clear();

                for(int x = written; x < batch.size(); x++)
                {
                    pending.put(batch.get(x).getKey(), batch.get(x).getValue());
                }

                pending.putAll(newer);
            }

            if(!isReachable(database))
            {
                LOGGER.error("Call database connection lost - will retry");
                closeDatabase();
                mErrorMessage = se.getMessage();
                mNextRetry = System.currentTimeMillis() + RETRY_INTERVAL_MS;
                broadcast(DatabaseEvent.Type.ERROR);
            }

            return false;
        }
    }

    private static void rollback(Connection connection)
    {
        try
        {
            connection.rollback();
        }
        catch(SQLException se)
        {
            LOGGER.warn("Database rollback failed - " + se.getMessage());
        }
    }

    private static void restoreAutoCommit(Connection connection, boolean autoCommit)
    {
        try
        {
            connection.setAutoCommit(autoCommit);
        }
        catch(SQLException se)
        {
            LOGGER.warn("Unable to restore database auto-commit - " + se.getMessage());
        }
    }

    private static boolean isReachable(CallDatabase database)
    {
        try(Connection connection = database.getConnection())
        {
            return connection.isValid(5);
        }
        catch(SQLException se)
        {
            return false;
        }
    }

    /**
     * Searches the calls table on the query thread.
     */
    public CompletableFuture<SearchResult> search(CallSearchCriteria criteria, int offset, int limit)
    {
        return CompletableFuture.supplyAsync(() -> {
            CallDatabase database = mDatabase;

            if(database == null)
            {
                throw new IllegalStateException("Call database is not available" +
                    (mErrorMessage != null ? ": " + mErrorMessage : ""));
            }

            try(Connection connection = database.getConnection())
            {
                List<CallRecord> calls = database.getCallRepository().search(connection, criteria, offset, limit);
                long total = database.getCallRepository().count(connection, criteria);
                return new SearchResult(calls, total);
            }
            catch(SQLException se)
            {
                throw new IllegalStateException(se.getMessage(), se);
            }
        }, mQueryExecutor);
    }

    /**
     * Search results
     * @param calls in the requested page
     * @param total count of matching calls
     */
    public record SearchResult(List<CallRecord> calls, long total) {}

    /**
     * Searches the events table on the query thread.
     */
    public CompletableFuture<EventSearchResult> searchEvents(EventSearchCriteria criteria, int offset, int limit)
    {
        return CompletableFuture.supplyAsync(() -> {
            CallDatabase database = mDatabase;

            if(database == null)
            {
                throw new IllegalStateException("Call database is not available" +
                    (mErrorMessage != null ? ": " + mErrorMessage : ""));
            }

            try(Connection connection = database.getConnection())
            {
                IEventRepository repository = database.getEventRepository();
                List<EventRecord> events = repository.search(connection, criteria, offset, limit);
                long total = repository.count(connection, criteria);
                return new EventSearchResult(events, total);
            }
            catch(SQLException se)
            {
                throw new IllegalStateException(se.getMessage(), se);
            }
        }, mQueryExecutor);
    }

    /**
     * Searches the stream activity table on the query thread.
     */
    public CompletableFuture<StreamActivitySearchResult> searchStreamActivity(StreamActivitySearchCriteria criteria,
                                                                              int offset, int limit)
    {
        return CompletableFuture.supplyAsync(() -> {
            CallDatabase database = mDatabase;

            if(database == null)
            {
                throw new IllegalStateException("Call database is not available" +
                    (mErrorMessage != null ? ": " + mErrorMessage : ""));
            }

            try(Connection connection = database.getConnection())
            {
                StreamActivityRepository repository = database.getStreamActivityRepository();
                return new StreamActivitySearchResult(repository.search(connection, criteria, offset, limit),
                    repository.count(connection, criteria), repository.getStreamNames(connection));
            }
            catch(SQLException se)
            {
                throw new IllegalStateException(se.getMessage(), se);
            }
        }, mQueryExecutor);
    }

    /**
     * Stream activity search results
     * @param records in the requested page
     * @param total count of matching rows
     * @param streamNames of all stored rows
     */
    public record StreamActivitySearchResult(List<StreamActivityRecord> records, long total,
                                             List<String> streamNames) {}

    /**
     * Event search results
     * @param events in the requested page
     * @param total count of matching events
     */
    public record EventSearchResult(List<EventRecord> events, long total) {}

    /**
     * Connects to the database configuration and reports the state of its schema.  Runs on the query thread.
     */
    public CompletableFuture<ProbeResult> probe(DatabaseConfiguration configuration)
    {
        return CompletableFuture.supplyAsync(() -> {
            try(CallDatabase database = new CallDatabase(configuration, getDatabaseDirectory()))
            {
                SchemaStatus status = database.getSchemaStatus();
                long count = status == SchemaStatus.SDRTRUNK ? database.getCallCount() : 0;
                long eventCount = status == SchemaStatus.SDRTRUNK ? database.getEventCount() : 0;
                String suggestion = status == SchemaStatus.MISSING ? configuration.schema() :
                    database.suggestSchemaName();
                return new ProbeResult(status, count, eventCount, suggestion);
            }
            catch(SQLException se)
            {
                throw new IllegalStateException(se.getMessage(), se);
            }
        }, mQueryExecutor);
    }

    /**
     * Result of probing a database configuration
     * @param status of the configured schema
     * @param callCount in the configured schema
     * @param eventCount in the configured schema
     * @param suggestedSchema an unused schema name, for the create separate schema option
     */
    public record ProbeResult(SchemaStatus status, long callCount, long eventCount, String suggestedSchema) {}

    /**
     * Checks whether a schema exists in the database of the configuration.  Runs on the query thread.
     */
    public CompletableFuture<Boolean> schemaExists(DatabaseConfiguration configuration, String schema)
    {
        return CompletableFuture.supplyAsync(() -> {
            try(CallDatabase database = new CallDatabase(configuration, getDatabaseDirectory()))
            {
                return database.schemaExists(schema);
            }
            catch(SQLException se)
            {
                throw new IllegalStateException(se.getMessage(), se);
            }
        }, mQueryExecutor);
    }

    /**
     * How to treat an existing sdrtrunk schema in the database being switched to
     */
    public enum ExistingSchemaOption
    {
        /**
         * Keep the existing calls and add to them
         */
        APPEND,
        /**
         * Remove the existing calls and start empty
         */
        CLEAR
    }

    /**
     * Switches to a different database.
     *
     * @param target configuration, including the schema name to use (a new name for the create separate schema option)
     * @param schemaOption for an existing sdrtrunk schema in the target
     * @param transfer true to copy the calls from the current database to the target
     * @param progress receives status text while the switch runs (on the database thread)
     * @return future that completes when the switch is complete, or completes exceptionally on error, in which case
     * the current database remains selected
     */
    public CompletableFuture<SwitchResult> switchDatabase(DatabaseConfiguration target,
                                                          ExistingSchemaOption schemaOption, boolean transfer,
                                                          Consumer<String> progress)
    {
        CompletableFuture<SwitchResult> future = new CompletableFuture<>();

        mDatabaseExecutor.execute(() -> {
            CallDatabase targetDatabase = null;

            try
            {
                progress.accept("Connecting to " + target.getDescription());
                targetDatabase = new CallDatabase(target, getDatabaseDirectory());
                SchemaStatus status = targetDatabase.getSchemaStatus();

                if(status == SchemaStatus.SDRTRUNK && schemaOption == ExistingSchemaOption.CLEAR)
                {
                    progress.accept("Clearing schema [" + target.schema() + "]");
                    targetDatabase.clear();
                }
                else
                {
                    progress.accept("Installing schema [" + target.schema() + "]");
                    targetDatabase.install();
                }

                long transferred = 0;
                long skipped = 0;
                long eventsTransferred = 0;
                long eventsSkipped = 0;
                CallDatabase current = mDatabase;

                if(transfer && current != null && !current.getConfiguration().isSameLocation(target))
                {
                    //Write pending updates first so that they are included in the transfer
                    flush();
                    long[] counts = transfer(current, targetDatabase, progress);
                    transferred = counts[0];
                    skipped = counts[1];
                    long[] eventCounts = transferEvents(current, targetDatabase, progress);
                    eventsTransferred = eventCounts[0];
                    eventsSkipped = eventCounts[1];
                }

                //Web API accounts always move with the database, so that a switch doesn't lock the users out.  They
                //are copied only when the target has no accounts of its own.
                if(current != null && !current.getConfiguration().isSameLocation(target))
                {
                    progress.accept("Copying web accounts");

                    try(Connection sourceConnection = current.getConnection();
                        Connection targetConnection = targetDatabase.getConnection())
                    {
                        long accounts = targetDatabase.getAccountRepository().copyFrom(
                            current.getAccountRepository(), sourceConnection, targetConnection);

                        if(accounts > 0)
                        {
                            LOGGER.info("Copied " + accounts + " web accounts to " + target.getDescription());
                        }
                    }

                    //Playlists also move with the database: the playlists that the target doesn't have (by name)
                    //are copied, and the playlist manager then writes the running playlist to the target.
                    progress.accept("Copying playlists");

                    try(Connection sourceConnection = current.getConnection();
                        Connection targetConnection = targetDatabase.getConnection())
                    {
                        boolean autoCommit = targetConnection.getAutoCommit();
                        targetConnection.setAutoCommit(false);

                        try
                        {
                            int playlists = targetDatabase.getPlaylistRepository().copyMissingFrom(
                                current.getPlaylistRepository(), sourceConnection, targetConnection);
                            targetConnection.commit();

                            if(playlists > 0)
                            {
                                LOGGER.info("Copied " + playlists + " playlists to " + target.getDescription());
                            }
                        }
                        catch(SQLException se)
                        {
                            targetConnection.rollback();
                            throw se;
                        }
                        finally
                        {
                            targetConnection.setAutoCommit(autoCommit);
                        }
                    }
                }

                try(Connection connection = targetDatabase.getConnection())
                {
                    targetDatabase.getCallRepository().markInterrupted(connection);
                }

                //Write remaining pending updates to the current database before switching, so that active calls
                //continue in the new database
                flush();
                CallDatabase previous = mDatabase;
                mDatabase = targetDatabase;
                mGeneration++;
                mErrorMessage = null;
                mSettings.getDatabaseSettings().setDatabaseConfiguration(target);

                if(previous != null)
                {
                    //Calls in progress continue in the new database, so remove them from the previous database
                    try(Connection connection = previous.getConnection())
                    {
                        previous.getCallRepository().deleteActive(connection);
                    }
                    catch(SQLException se)
                    {
                        LOGGER.warn("Unable to remove calls in progress from the previous database - " +
                            se.getMessage());
                    }

                    previous.close();
                }

                LOGGER.info("Switched call database to " + target.getDescription() + " - transferred " +
                    transferred + " calls" + (skipped > 0 ? ", skipped " + skipped + " existing calls" : "") +
                    " and " + eventsTransferred + " events" + (eventsSkipped > 0 ? ", skipped " + eventsSkipped +
                    " existing events" : ""));
                broadcast(DatabaseEvent.Type.CONNECTED);
                broadcast(DatabaseEvent.Type.SWITCHED);
                future.complete(new SwitchResult(transferred, skipped, eventsTransferred, eventsSkipped));
            }
            catch(Exception e)
            {
                LOGGER.error("Error switching the call database to " + target.getDescription(), e);

                if(targetDatabase != null && targetDatabase != mDatabase)
                {
                    targetDatabase.close();
                }

                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Result of a database switch
     * @param transferred count of calls copied to the new database
     * @param skipped count of calls that already existed in the new database
     * @param eventsTransferred count of events copied to the new database
     * @param eventsSkipped count of events that already existed in the new database
     */
    public record SwitchResult(long transferred, long skipped, long eventsTransferred, long eventsSkipped) {}

    /**
     * Copies the calls from the source to the target database, skipping calls in progress (they are written to the
     * target after the switch) and calls that already exist in the target.
     * @return transferred and skipped counts
     */
    private long[] transfer(CallDatabase source, CallDatabase target, Consumer<String> progress) throws SQLException
    {
        ICallRepository sourceRepository = source.getCallRepository();
        ICallRepository targetRepository = target.getCallRepository();
        long transferred = 0;
        long skipped = 0;

        try(Connection sourceConnection = source.getConnection(); Connection targetConnection = target.getConnection())
        {
            long total = sourceRepository.count(sourceConnection);
            Set<String> existing = targetRepository.count(targetConnection) > 0 ?
                targetRepository.getTransferKeys(targetConnection) : Collections.emptySet();
            boolean autoCommit = targetConnection.getAutoCommit();
            targetConnection.setAutoCommit(false);

            try
            {
                long afterId = 0;
                List<CallRecord> page = sourceRepository.readPage(sourceConnection, afterId, TRANSFER_PAGE_SIZE);

                while(!page.isEmpty())
                {
                    List<CallRecord> toInsert = new ArrayList<>();

                    for(CallRecord call: page)
                    {
                        if(call.getCallState() == CallState.ACTIVE || existing.contains(call.getTransferKey()))
                        {
                            skipped++;
                        }
                        else
                        {
                            toInsert.add(call);
                        }
                    }

                    if(!toInsert.isEmpty())
                    {
                        targetRepository.insertBatch(targetConnection, toInsert);
                        targetConnection.commit();
                        transferred += toInsert.size();
                    }

                    afterId = page.getLast().getId();
                    progress.accept("Transferring calls: " + (transferred + skipped) + " of " + total);
                    page = sourceRepository.readPage(sourceConnection, afterId, TRANSFER_PAGE_SIZE);
                }
            }
            catch(SQLException se)
            {
                targetConnection.rollback();
                throw se;
            }
            finally
            {
                targetConnection.setAutoCommit(autoCommit);
            }
        }

        return new long[]{transferred, skipped};
    }

    /**
     * Copies the events from the source to the target database, skipping events that already exist in the target.
     * @return transferred and skipped counts
     */
    private long[] transferEvents(CallDatabase source, CallDatabase target, Consumer<String> progress)
        throws SQLException
    {
        IEventRepository sourceRepository = source.getEventRepository();
        IEventRepository targetRepository = target.getEventRepository();
        long transferred = 0;
        long skipped = 0;

        try(Connection sourceConnection = source.getConnection(); Connection targetConnection = target.getConnection())
        {
            long total = sourceRepository.count(sourceConnection);
            Set<String> existing = targetRepository.count(targetConnection) > 0 ?
                targetRepository.getTransferKeys(targetConnection) : Collections.emptySet();
            boolean autoCommit = targetConnection.getAutoCommit();
            targetConnection.setAutoCommit(false);

            try
            {
                List<EventRecord> page = sourceRepository.readPage(sourceConnection, 0, TRANSFER_PAGE_SIZE);

                while(!page.isEmpty())
                {
                    List<EventRecord> toInsert = new ArrayList<>();

                    for(EventRecord event: page)
                    {
                        if(existing.contains(event.getTransferKey()))
                        {
                            skipped++;
                        }
                        else
                        {
                            toInsert.add(event);
                        }
                    }

                    if(!toInsert.isEmpty())
                    {
                        targetRepository.insertBatch(targetConnection, toInsert);
                        targetConnection.commit();
                        transferred += toInsert.size();
                    }

                    progress.accept("Transferring events: " + (transferred + skipped) + " of " + total);
                    page = sourceRepository.readPage(sourceConnection, page.getLast().getId(), TRANSFER_PAGE_SIZE);
                }
            }
            catch(SQLException se)
            {
                targetConnection.rollback();
                throw se;
            }
            finally
            {
                targetConnection.setAutoCommit(autoCommit);
            }
        }

        return new long[]{transferred, skipped};
    }

    /**
     * Restores the backup into the current database, replacing its calls and events.  Runs on the database thread,
     * after writing the pending updates.  Calls and events in progress continue as new rows.  Listeners receive a
     * CONNECTED event when the restore is complete.
     * @param backup file to restore
     * @param progress receives status text
     */
    public CompletableFuture<BackupManager.RestoreResult> restore(Path backup, Consumer<String> progress)
    {
        CompletableFuture<BackupManager.RestoreResult> future = new CompletableFuture<>();

        mDatabaseExecutor.execute(() -> {
            try
            {
                CallDatabase database = mDatabase;

                if(database == null)
                {
                    throw new IllegalStateException("The call database is not available");
                }

                flush();
                BackupManager.RestoreResult result = BackupManager.restore(database, backup, progress);

                //Rows of calls and events in progress were replaced - write them as new rows
                mGeneration++;
                broadcast(DatabaseEvent.Type.CONNECTED);

                if(result.playlists() >= 0)
                {
                    mPlaylistRestoreCount++;
                    broadcast(DatabaseEvent.Type.PLAYLISTS_RESTORED);
                }

                future.complete(result);
            }
            catch(Exception e)
            {
                LOGGER.error("Error restoring database backup [" + backup + "]", e);
                future.completeExceptionally(e);
            }
        });

        return future;
    }

    /**
     * Backup manager
     */
    public BackupManager getBackupManager()
    {
        return mBackupManager;
    }

    /**
     * Age-off rule manager
     */
    public AgeOffManager getAgeOffManager()
    {
        return mAgeOffManager;
    }

    /**
     * Count of restores that replaced the playlists in this session.  The playlist manager compares it to the count
     * when it loaded the playlist, so that it never writes the running playlist over restored playlists.
     */
    public int getPlaylistRestoreCount()
    {
        return mPlaylistRestoreCount;
    }

    /**
     * Work that runs on the database thread against the open database
     */
    @FunctionalInterface
    public interface DatabaseTask<T>
    {
        T run(CallDatabase database) throws Exception;
    }

    /**
     * Runs the task on the database thread against the open database.
     * @return future with the result of the task, or that completes exceptionally with the task's exception or with
     * an IllegalStateException when the database isn't available
     */
    public <T> CompletableFuture<T> submit(DatabaseTask<T> task)
    {
        CompletableFuture<T> future = new CompletableFuture<>();

        try
        {
            mDatabaseExecutor.execute(() -> {
                CallDatabase database = mDatabase;

                if(database == null)
                {
                    future.completeExceptionally(new IllegalStateException("The database is not available" +
                        (mErrorMessage != null ? " - " + mErrorMessage : "")));
                    return;
                }

                try
                {
                    future.complete(task.run(database));
                }
                catch(Throwable t)
                {
                    future.completeExceptionally(t);
                }
            });
        }
        catch(RejectedExecutionException ree)
        {
            future.completeExceptionally(new IllegalStateException("The database is closed"));
        }

        return future;
    }

    /**
     * Runs the task on the database thread.
     */
    public CompletableFuture<Void> runOnDatabaseThread(Runnable runnable)
    {
        return CompletableFuture.runAsync(runnable, mDatabaseExecutor);
    }

    /**
     * Notifies listeners that the stored calls changed outside of the normal call updates (age-off, switch, delete).
     */
    public void notifyCallsChanged()
    {
        broadcast(DatabaseEvent.Type.CALLS_CHANGED);
    }

    /**
     * Notifies listeners that the stored events changed outside of the normal event updates (age-off).
     */
    public void notifyEventsChanged()
    {
        broadcast(DatabaseEvent.Type.EVENTS_CHANGED);
    }

    /**
     * Notifies listeners that the stored stream activity changed outside of the live updates (age-off).
     */
    public void notifyStreamActivityChanged()
    {
        broadcast(DatabaseEvent.Type.STREAM_ACTIVITY_CHANGED);
    }

    private void broadcast(DatabaseEvent.Type type)
    {
        mEventBroadcaster.broadcast(new DatabaseEvent(type, getConfiguration(), mErrorMessage));
    }

    /**
     * Registers the listener to receive database events (on the database thread)
     */
    public void addListener(Listener<DatabaseEvent> listener)
    {
        mEventBroadcaster.addListener(listener);
    }

    public void removeListener(Listener<DatabaseEvent> listener)
    {
        mEventBroadcaster.removeListener(listener);
    }

    /**
     * Registers the listener to receive each call after it is written to the database, with its row id assigned.
     * Notified on the database thread.
     */
    public void addCallSavedListener(Listener<CallRecord> listener)
    {
        mSavedCallBroadcaster.addListener(listener);
    }

    public void removeCallSavedListener(Listener<CallRecord> listener)
    {
        mSavedCallBroadcaster.removeListener(listener);
    }

    /**
     * Registers the listener to receive each event after it is written to the database, with its row id assigned.
     * Notified on the database thread.
     */
    public void addEventSavedListener(Listener<EventRecord> listener)
    {
        mSavedEventBroadcaster.addListener(listener);
    }

    public void removeEventSavedListener(Listener<EventRecord> listener)
    {
        mSavedEventBroadcaster.removeListener(listener);
    }

    /**
     * Tracks the database row of a call or event.  Accessed only on the database thread.
     */
    public static class RowHandle
    {
        private long mId;
        private int mGeneration;
    }
}
