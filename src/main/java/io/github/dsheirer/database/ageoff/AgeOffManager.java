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
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.database.DatabasePreference;
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
    private final DatabasePreference mPreference;
    private ScheduledFuture<?> mScheduledFuture;

    /**
     * Constructs an instance
     */
    public AgeOffManager(DatabaseManager databaseManager, UserPreferences userPreferences)
    {
        mDatabaseManager = databaseManager;
        mPreference = userPreferences.getDatabasePreference();
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

        RetentionPeriod retention = mPreference.getDuplicateDoNotMonitorRetention();

        if(database == null || (!retention.isEnabled() && !mPreference.isAgeRuleEnabled() &&
            !mPreference.isSizeRuleEnabled()))
        {
            return new AgeOffResult(0, 0, 0);
        }

        AgeOffResult result = new AgeOffResult(0, 0, 0);

        try(Connection connection = database.getConnection())
        {
            ICallRepository repository = database.getCallRepository();

            if(retention.isEnabled())
            {
                long cutoff = System.currentTimeMillis() - retention.getMilliseconds();
                result = result.add(applyRetentionRule(connection, repository, cutoff));
            }

            if(mPreference.isAgeRuleEnabled())
            {
                long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(mPreference.getAgeRuleDays());
                result = result.add(applyAgeRule(connection, repository, cutoff, mPreference.getAgeRuleAction()));
            }

            if(mPreference.isSizeRuleEnabled())
            {
                long limit = mPreference.getSizeRuleLimitMB() * BYTES_PER_MB;
                result = result.add(applySizeRule(connection, repository, limit, mPreference.getSizeRuleAction()));
            }
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
    static AgeOffResult applyRetentionRule(Connection connection, ICallRepository repository, long cutoff)
        throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0);
        List<CallRecord> page = repository.getDuplicateAndDoNotMonitorCalls(connection, cutoff, PAGE_SIZE);

        while(!page.isEmpty())
        {
            result = result.add(remove(connection, repository, page, AgeOffAction.DELETE_CALL_AND_RECORDING));
            page = repository.getDuplicateAndDoNotMonitorCalls(connection, cutoff, PAGE_SIZE);
        }

        return result;
    }

    /**
     * Removes the calls (or recordings) of calls that started before the cutoff time.
     */
    static AgeOffResult applyAgeRule(Connection connection, ICallRepository repository, long cutoff,
                                     AgeOffAction action) throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0);
        List<CallRecord> page = action == AgeOffAction.DELETE_CALL_AND_RECORDING ?
            repository.getCallsStartedBefore(connection, cutoff, PAGE_SIZE) :
            repository.getRecordedCalls(connection, cutoff, PAGE_SIZE);

        while(!page.isEmpty())
        {
            result = result.add(remove(connection, repository, page, action));

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
                                      AgeOffAction action) throws SQLException
    {
        AgeOffResult result = new AgeOffResult(0, 0, 0);
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

            result = result.add(remove(connection, repository, toRemove, action));
        }

        return result;
    }

    /**
     * Deletes the recording files of the calls and then deletes the call rows or marks their recordings deleted.
     */
    private static AgeOffResult remove(Connection connection, ICallRepository repository, List<CallRecord> calls,
                                       AgeOffAction action) throws SQLException
    {
        long bytes = 0;
        int recordings = 0;
        List<Long> ids = new ArrayList<>();

        for(CallRecord call: calls)
        {
            ids.add(call.getId());

            if(call.getFilePath() != null && deleteRecording(call.getFilePath()))
            {
                recordings++;
                bytes += call.getFileSize();
            }
        }

        if(action == AgeOffAction.DELETE_CALL_AND_RECORDING)
        {
            repository.delete(connection, ids);
            return new AgeOffResult(ids.size(), recordings, bytes);
        }

        repository.markRecordingsDeleted(connection, ids);
        return new AgeOffResult(0, recordings, bytes);
    }

    /**
     * Deletes the audio recording file.  Only audio files (.mp3, .wav) are deleted.
     * @return true if the file was deleted
     */
    static boolean deleteRecording(String filePath)
    {
        String lower = filePath.toLowerCase(Locale.ROOT);

        if(!lower.endsWith(".mp3") && !lower.endsWith(".wav"))
        {
            LOGGER.warn("Age-off skipped deleting non-audio file [" + filePath + "]");
            return false;
        }

        try
        {
            return Files.deleteIfExists(Path.of(filePath));
        }
        catch(IOException | RuntimeException e)
        {
            LOGGER.warn("Age-off unable to delete recording [" + filePath + "] - " + e.getMessage());
            return false;
        }
    }

    /**
     * Age-off results
     * @param calls deleted
     * @param recordings deleted
     * @param bytes of recordings deleted
     */
    public record AgeOffResult(long calls, long recordings, long bytes)
    {
        public AgeOffResult add(AgeOffResult other)
        {
            return new AgeOffResult(calls + other.calls, recordings + other.recordings, bytes + other.bytes);
        }
    }
}
