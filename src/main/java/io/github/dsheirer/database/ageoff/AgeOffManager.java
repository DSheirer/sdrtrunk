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
package io.github.dsheirer.database.ageoff;

import io.github.dsheirer.database.CallDatabase;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.database.call.CallRecord;
import io.github.dsheirer.database.call.ICallRepository;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.database.DatabaseSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Applies the call audio age-off rules:
 * <ul>
 *     <li>Retention rule: deletes duplicate calls, and Do Not Monitor calls that no alias marked for recording, with
 *     their recordings, after the retention period (default 1 hour).</li>
 *     <li>Age rule: removes calls older than a number of days.</li>
 *     <li>Size rule: removes the oldest recordings when the total size of the recordings exceeds a limit.</li>
 *     <li>Event rule: deletes decode events older than a number of days (enabled by default, 30 days).</li>
 *     <li>Stream activity rule: deletes the Streaming tab rows of calls older than a number of days (enabled by
 *     default, 3 days).</li>
 * </ul>
 * The age and size rules either delete the call row and its recording file, or delete only the recording file and
 * keep the call row.
 *
 * The rules run a minute after startup, then every 5 minutes, and when the user requests it.  Calls in progress are
 * never removed.  Only recordings tracked in the call database are considered.
 */
public class AgeOffManager
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AgeOffManager.class);
    private static final long INITIAL_DELAY_SECONDS = 60;
    private static final long INTERVAL_SECONDS = 300;
    private static final int PAGE_SIZE = 500;
    public static final long BYTES_PER_MB = 1024L * 1024L;

    private final DatabaseManager mDatabaseManager;
    private final DatabaseSettings mDatabaseSettings;
    private final Settings mSettings;
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * Constructs an instance
     */
    public AgeOffManager(DatabaseManager databaseManager, Settings settings)
    {
        mDatabaseManager = databaseManager;
        mSettings = settings;
        mDatabaseSettings = settings.getDatabaseSettings();
    }

    /**
     * Schedules the periodic age-off on the database thread.
     */
    public void start(ScheduledExecutorService databaseExecutor)
    {
        if(mScheduledFuture == null)
        {
            mScheduledFuture = databaseExecutor.scheduleWithFixedDelay(this::runSafely, INITIAL_DELAY_SECONDS,
                INTERVAL_SECONDS, TimeUnit.SECONDS);
        }
    }

    public void stop()
    {
        if(mScheduledFuture != null)
        {
            mScheduledFuture.cancel(false);
            mScheduledFuture = null;
        }
    }

    /**
     * Runs the enabled rules now.
     * @return future with the results
     */
    public CompletableFuture<AgeOffResult> runNow()
    {
        CompletableFuture<AgeOffResult> future = new CompletableFuture<>();
        mDatabaseManager.runOnDatabaseThread(() -> {
            try
            {
                future.complete(run());
            }
            catch(Exception e)
            {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private void runSafely()
    {
        try
        {
            run();
        }
        catch(Exception e)
        {
            LOGGER.error("Error applying the call age-off rules", e);
        }
    }

    /**
     * Applies the enabled rules.  Database thread only.
     */
    private AgeOffResult run() throws SQLException
    {
        CallDatabase database = mDatabaseManager.getDatabase();

        RetentionPeriod retention = mDatabaseSettings.getDuplicateDoNotMonitorRetention();

        if(database == null || (!retention.isEnabled() && !mDatabaseSettings.isAgeRuleEnabled() &&
            !mDatabaseSettings.isSizeRuleEnabled() && !mDatabaseSettings.isEventRuleEnabled() &&
            !mDatabaseSettings.isStreamActivityRuleEnabled()))
        {
            return new AgeOffResult(0, 0, 0, 0, 0);
        }

        AgeOffResult result = new AgeOffResult(0, 0, 0, 0, 0);
        long events = 0;
        long streamActivity = 0;
        Path root = mSettings.getDirectorySettings().getDirectoryRecording();

        try(Connection connection = database.getConnection())
        {
            ICallRepository repository = database.getCallRepository();

            if(retention.isEnabled())
            {
                long cutoff = System.currentTimeMillis() - retention.getMilliseconds();
                result = result.add(applyRetentionRule(connection, repository, cutoff, root));
            }

            if(mDatabaseSettings.isAgeRuleEnabled())
            {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(mDatabaseSettings.getAgeRuleDays());
                result = result.add(applyAgeRule(connection, repository, cutoff, mDatabaseSettings.getAgeRuleAction(),
                    root));
            }

            if(mDatabaseSettings.isSizeRuleEnabled())
            {
                long limit = mDatabaseSettings.getSizeRuleLimitMB() * BYTES_PER_MB;
                result = result.add(applySizeRule(connection, repository, limit, mDatabaseSettings.getSizeRuleAction(),
                    root));
            }

            if(mDatabaseSettings.isEventRuleEnabled())
            {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(mDatabaseSettings.getEventRuleDays());
                events = database.getEventRepository().deleteStartedBefore(connection, cutoff);
                result = result.add(new AgeOffResult(0, 0, 0, events, 0));
            }

            if(mDatabaseSettings.isStreamActivityRuleEnabled())
            {
                long cutoff = System.currentTimeMillis() -
                    TimeUnit.DAYS.toMillis(mDatabaseSettings.getStreamActivityRuleDays());
                streamActivity = database.getStreamActivityRepository().deleteStartedBefore(connection, cutoff);
                result = result.add(new AgeOffResult(0, 0, 0, 0, streamActivity));
            }
        }

        if(events > 0)
        {
            LOGGER.info("Event age-off removed " + events + " events");
            mDatabaseManager.notifyEventsChanged();
        }

        if(streamActivity > 0)
        {
            LOGGER.info("Stream activity age-off removed " + streamActivity + " streaming rows");
            mDatabaseManager.notifyStreamActivityChanged();
        }

        if(result.calls() > 0 || result.recordings() > 0)
        {
            LOGGER.info("Call age-off removed " + result.calls() + " calls and " + result.recordings() +
                " recordings (" + (result.bytes() / BYTES_PER_MB) + " MB)");
            mDatabaseManager.notifyCallsChanged();
        }

        return result;
    }

    /**
     * Deletes duplicate calls and Do Not Monitor calls (without an alias record option) that started before the
     * cutoff time, with their recordings.
     */
    static AgeOffResult applyRetentionRule(Connection connection, ICallRepository repository, long cutoff,
                                           Path root)
        throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0, 0, 0);
        List<CallRecord> page = repository.getDuplicateAndDoNotMonitorCalls(connection, cutoff, PAGE_SIZE);

        while(!page.isEmpty())
        {
            result = result.add(remove(connection, repository, page, AgeOffAction.DELETE_CALL_AND_RECORDING,
                root));
            page = repository.getDuplicateAndDoNotMonitorCalls(connection, cutoff, PAGE_SIZE);
        }

        return result;
    }

    /**
     * Removes the calls (or recordings) of calls that started before the cutoff time.
     */
    static AgeOffResult applyAgeRule(Connection connection, ICallRepository repository, long cutoff,
                                     AgeOffAction action, Path root) throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0, 0, 0);
        List<CallRecord> page = action == AgeOffAction.DELETE_CALL_AND_RECORDING ?
            repository.getCallsStartedBefore(connection, cutoff, PAGE_SIZE) :
            repository.getRecordedCalls(connection, cutoff, PAGE_SIZE);

        while(!page.isEmpty())
        {
            result = result.add(remove(connection, repository, page, action, root));

            page = action == AgeOffAction.DELETE_CALL_AND_RECORDING ?
                repository.getCallsStartedBefore(connection, cutoff, PAGE_SIZE) :
                repository.getRecordedCalls(connection, cutoff, PAGE_SIZE);
        }

        return result;
    }

    /**
     * Removes the oldest recordings until the total size of the recordings is at or below the limit.
     */
    static AgeOffResult applySizeRule(Connection connection, ICallRepository repository, long limitBytes,
                                      AgeOffAction action, Path root) throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0, 0, 0);
        long total = repository.getRecordedFileSize(connection);

        while(total > limitBytes)
        {
            List<CallRecord> page = repository.getRecordedCalls(connection, 0, PAGE_SIZE);

            if(page.isEmpty())
            {
                break;
            }

            List<CallRecord> toRemove = new ArrayList<>();

            for(CallRecord call: page)
            {
                if(total <= limitBytes)
                {
                    break;
                }

                toRemove.add(call);
                total -= call.getFileSize();
            }

            result = result.add(remove(connection, repository, toRemove, action, root));
        }

        return result;
    }

    /**
     * Deletes the recording files of the calls and then deletes the call rows or marks their recordings deleted.
     */
    private static AgeOffResult remove(Connection connection, ICallRepository repository, List<CallRecord> calls,
                                       AgeOffAction action, Path root) throws SQLException
    {
        long bytes = 0;
        int recordings = 0;
        List<Long> ids = new ArrayList<>();

        for(CallRecord call: calls)
        {
            ids.add(call.getId());

            if(call.getFilePath() != null && deleteRecording(call.getFilePath(), root))
            {
                recordings++;
                bytes += call.getFileSize();
            }
        }

        if(action == AgeOffAction.DELETE_CALL_AND_RECORDING)
        {
            repository.delete(connection, ids);
            return new AgeOffResult(ids.size(), recordings, bytes, 0, 0);
        }

        repository.markRecordingsDeleted(connection, ids);
        return new AgeOffResult(0, recordings, bytes, 0, 0);
    }

    /**
     * Deletes the audio recording file.  Only audio files (.mp3, .wav) are deleted.
     * @return true if the file was deleted
     */
    static boolean deleteRecording(String filePath, Path root)
    {
        String lower = filePath.toLowerCase(Locale.ROOT);

        if(!lower.endsWith(".mp3") && !lower.endsWith(".wav"))
        {
            LOGGER.warn("Age-off skipped deleting non-audio file [" + filePath + "]");
            return false;
        }

        try
        {
            Path path = Path.of(filePath);
            boolean deleted = Files.deleteIfExists(path);
            deleteEmptyFolders(path.getParent(), root);
            return deleted;
        }
        catch(IOException | RuntimeException e)
        {
            LOGGER.warn("Age-off unable to delete recording [" + filePath + "] - " + e.getMessage());
            return false;
        }
    }

    /**
     * Deletes the folder and its parent folders while they are empty, up to (not including) the recordings folder.
     * Folders outside the recordings folder are never deleted.
     * @param folder of a deleted recording
     * @param root recordings folder, or null to not delete any folders
     */
    static void deleteEmptyFolders(Path folder, Path root)
    {
        if(folder == null || root == null)
        {
            return;
        }

        Path rootPath = root.toAbsolutePath().normalize();
        Path current = folder.toAbsolutePath().normalize();

        while(current != null && current.startsWith(rootPath) && !current.equals(rootPath))
        {
            try(var entries = Files.list(current))
            {
                if(entries.findAny().isPresent())
                {
                    return;
                }
            }
            catch(IOException | RuntimeException e)
            {
                return;
            }

            try
            {
                Files.delete(current);
            }
            catch(IOException | RuntimeException e)
            {
                //Not empty (a new recording was just started in it) or not accessible
                return;
            }

            current = current.getParent();
        }
    }

    /**
     * Age-off results
     * @param calls deleted
     * @param recordings deleted
     * @param bytes of recordings deleted
     * @param events deleted
     * @param streamActivity rows deleted
     */
    public record AgeOffResult(long calls, long recordings, long bytes, long events, long streamActivity)
    {
        public AgeOffResult add(AgeOffResult other)
        {
            return new AgeOffResult(calls + other.calls, recordings + other.recordings, bytes + other.bytes,
                events + other.events, streamActivity + other.streamActivity);
        }
    }
}
