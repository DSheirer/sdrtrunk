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
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.ICallRepository;
import io.github.dsheirer.database.call.CallSearchCriteria;
import io.github.dsheirer.database.call.CallState;
import io.github.dsheirer.preference.UserPreferences;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the call database: opens the selected database at startup, writes call records in the background, runs the
 * age-off rules and switches between databases (with optional transfer of the existing calls).
 *
 * All writes, the age-off rules and database switches run on a single database thread, so they never overlap.
 * Searches run on a separate thread so that a long write doesn't block the user interface.
 *
 * Call recording doesn't depend on the database: when the database is unavailable, call updates are held in memory
 * (newest update per call) and written when the database becomes available again.
 */
public class DatabaseManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);
    private static final long FLUSH_INTERVAL_MS = 250;
    private static final long RETRY_INTERVAL_MS = 30_000;
    private static final int MAX_PENDING_CALLS = 20_000;
    private static final int TRANSFER_PAGE_SIZE = 1000;

    private final UserPreferences mUserPreferences;
    private final ScheduledExecutorService mDatabaseExecutor =
        Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk call database"));
    private final ExecutorService mQueryExecutor =
        Executors.newSingleThreadExecutor(new NamingThreadFactory("sdrtrunk call database query"));
    private final Broadcaster<DatabaseEvent> mEventBroadcaster = new Broadcaster<>();
    private final Broadcaster<CallRecord> mSavedCallBroadcaster = new Broadcaster<>();
    private final Map<CallHandle,CallRecord> mPendingCalls = new LinkedHashMap<>();
    private final AgeOffManager mAgeOffManager;
    private volatile CallDatabase mDatabase;
    private volatile String mErrorMessage;
    private volatile int mGeneration = 1;
    private long mNextRetry;
    private ScheduledFuture<?> mFlushFuture;

    /**
     * Constructs an instance
     * @param userPreferences for the database selection and age-off rules
     */
    public DatabaseManager(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
        mAgeOffManager = new AgeOffManager(this, userPreferences);
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
    }

    /**
     * Writes any remaining call updates and closes the database.
     */
    public void stop()
    {
        mAgeOffManager.stop();

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
        return mUserPreferences.getDirectoryPreference().getDirectoryDatabase();
    }

    /**
     * Opens the database from the user preferences.  Database thread only.
     */
    private void openConfiguredDatabase()
    {
        DatabaseConfiguration configuration = mUserPreferences.getDatabasePreference().getDatabaseConfiguration();

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
            mUserPreferences.getDatabasePreference().getDatabaseConfiguration();
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
     * Creates a handle that tracks the database row of one call across updates.
     */
    public CallHandle createHandle()
    {
        return new CallHandle();
    }

    /**
     * Queues a snapshot of the call for insertion (first time) or update.  Only the newest snapshot for each call is
     * written, so this can be invoked as often as the call changes.
     * @param handle for the call
     * @param snapshot of the call that is not modified after this call
     */
    public void save(CallHandle handle, CallRecord snapshot)
    {
        synchronized(mPendingCalls)
        {
            mPendingCalls.put(handle, snapshot);

            //Bound memory use if the database is unavailable for a long time.  Drop the oldest completed calls first.
            if(mPendingCalls.size() > MAX_PENDING_CALLS)
            {
                Iterator<Map.Entry<CallHandle,CallRecord>> it = mPendingCalls.entrySet().iterator();

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
     * Writes the pending call updates.  Database thread only.
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

        List<Map.Entry<CallHandle,CallRecord>> batch;

        synchronized(mPendingCalls)
        {
            if(mPendingCalls.isEmpty())
            {
                return;
            }

            batch = new ArrayList<>(mPendingCalls.entrySet());
            mPendingCalls.clear();
        }

        ICallRepository repository = database.getCallRepository();
        int written = 0;

        try(Connection connection = database.getConnection())
        {
            for(Map.Entry<CallHandle,CallRecord> entry: batch)
            {
                CallHandle handle = entry.getKey();
                CallRecord call = entry.getValue();

                if(handle.mGeneration == mGeneration && handle.mId > 0)
                {
                    call.setId(handle.mId);

                    if(repository.update(connection, call))
                    {
                        written++;
                        mSavedCallBroadcaster.broadcast(call);
                        continue;
                    }
                }

                //New call, a call that started before a database switch, or a row removed by an age-off rule.
                call.setId(0);
                handle.mId = repository.insert(connection, call);
                handle.mGeneration = mGeneration;
                written++;
                mSavedCallBroadcaster.broadcast(call);
            }
        }
        catch(SQLException se)
        {
            LOGGER.error("Error writing call updates to the database - " + se.getMessage());

            //Put back the updates that weren't written, unless a newer update arrived in the meantime
            synchronized(mPendingCalls)
            {
                Map<CallHandle,CallRecord> newer = new LinkedHashMap<>(mPendingCalls);
                mPendingCalls.clear();

                for(int x = written; x < batch.size(); x++)
                {
                    mPendingCalls.put(batch.get(x).getKey(), batch.get(x).getValue());
                }

                mPendingCalls.putAll(newer);
            }

            if(!isReachable(database))
            {
                LOGGER.error("Call database connection lost - will retry");
                closeDatabase();
                mErrorMessage = se.getMessage();
                mNextRetry = System.currentTimeMillis() + RETRY_INTERVAL_MS;
                broadcast(DatabaseEvent.Type.ERROR);
            }
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
     * Connects to the database configuration and reports the state of its schema.  Runs on the query thread.
     */
    public CompletableFuture<ProbeResult> probe(DatabaseConfiguration configuration)
    {
        return CompletableFuture.supplyAsync(() -> {
            try(CallDatabase database = new CallDatabase(configuration, getDatabaseDirectory()))
            {
                SchemaStatus status = database.getSchemaStatus();
                long count = status == SchemaStatus.SDRTRUNK ? database.getCallCount() : 0;
                String suggestion = status == SchemaStatus.MISSING ? configuration.schema() :
                    database.suggestSchemaName();
                return new ProbeResult(status, count, suggestion);
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
     * @param suggestedSchema an unused schema name, for the create separate schema option
     */
    public record ProbeResult(SchemaStatus status, long callCount, String suggestedSchema) {}

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
                CallDatabase current = mDatabase;

                if(transfer && current != null && !current.getConfiguration().isSameLocation(target))
                {
                    //Write pending updates first so that they are included in the transfer
                    flush();
                    long[] counts = transfer(current, targetDatabase, progress);
                    transferred = counts[0];
                    skipped = counts[1];
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
                mUserPreferences.getDatabasePreference().setDatabaseConfiguration(target);

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
                    transferred + " calls" + (skipped > 0 ? ", skipped " + skipped + " existing calls" : ""));
                broadcast(DatabaseEvent.Type.CONNECTED);
                future.complete(new SwitchResult(transferred, skipped));
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
     */
    public record SwitchResult(long transferred, long skipped) {}

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
     * Age-off rule manager
     */
    public AgeOffManager getAgeOffManager()
    {
        return mAgeOffManager;
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
     * Tracks the database row of a call.  Accessed only on the database thread.
     */
    public static class CallHandle
    {
        private long mId;
        private int mGeneration;
    }
}
