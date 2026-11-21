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

package io.github.dsheirer.monitor;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.database.DatabaseSettings;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleObjectProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Monitors the application's resource usage for the status bar:
 * - CPU usage of the sdrtrunk process
 * - Java heap memory usage
 * - Drive space used by the recordings and event logs folders, against the limits set in the storage settings
 *
 * The properties are updated on the JavaFX thread.  Monitoring only runs between start() and stop(), so that it
 * costs nothing while the status bar isn't showing.
 */
public class ResourceMonitor
{
    private static final Logger sLog = LoggerFactory.getLogger(ResourceMonitor.class);
    private static final LoggingSuppressor sLogSuppressor = new LoggingSuppressor(sLog);
    private static final long BYTES_PER_MEGABYTE = 1024L * 1024L;
    //Walking a large recordings folder is costly, so storage is scanned rarely and when the storage settings change
    private static final long STORAGE_SCAN_INTERVAL_SECONDS = 120;
    //CPU usage is averaged over this many one second samples for the warning level, so that a spike doesn't flash
    private static final int CPU_LEVEL_SAMPLES = 5;
    //Used heap rises until each garbage collection, so the warning level is the lowest used over this many seconds,
    //which approximates the memory still in use after collection
    private static final int MEMORY_LEVEL_SAMPLES = 15;

    private final Settings mSettings;
    private final com.sun.management.OperatingSystemMXBean mOperatingSystemMXBean;
    private final AtomicBoolean mStorageScanRunning = new AtomicBoolean();
    private final Deque<Double> mCpuSamples = new ArrayDeque<>();
    private final Deque<Long> mMemorySamples = new ArrayDeque<>();
    private volatile DatabaseSettings mDatabaseSettings;
    private ScheduledFuture<?> mMemoryCpuMonitorFuture;
    private ScheduledFuture<?> mStorageMonitorFuture;
    private boolean mRunning;

    private final LongProperty mMemoryMax = new SimpleLongProperty();
    private final LongProperty mMemoryAllocated = new SimpleLongProperty();
    private final LongProperty mMemoryUsed = new SimpleLongProperty();
    private final DoubleProperty mMemoryUsedPercentage = new SimpleDoubleProperty();
    private final DoubleProperty mMemoryLevelPercentage = new SimpleDoubleProperty();
    private final DoubleProperty mCpuPercentage = new SimpleDoubleProperty();
    private final DoubleProperty mCpuLevelPercentage = new SimpleDoubleProperty();
    private final DoubleProperty mSystemCpuPercentage = new SimpleDoubleProperty(-1);
    private final BooleanProperty mCpuAvailable = new SimpleBooleanProperty(true);
    private final ObjectProperty<StorageUsage> mRecordingsUsage = new SimpleObjectProperty<>();
    private final ObjectProperty<StorageUsage> mEventLogsUsage = new SimpleObjectProperty<>();

    /**
     * Constructs an instance
     * @param settings for the storage folders and their limits
     */
    public ResourceMonitor(Settings settings)
    {
        mSettings = settings;
        mOperatingSystemMXBean = getOperatingSystemMXBean();
        mMemoryMax.set(Runtime.getRuntime().maxMemory());
        MyEventBus.getGlobalEventBus().register(this);
    }

    private static com.sun.management.OperatingSystemMXBean getOperatingSystemMXBean()
    {
        try
        {
            OperatingSystemMXBean bean = ManagementFactory.getOperatingSystemMXBean();

            if(bean instanceof com.sun.management.OperatingSystemMXBean sunBean)
            {
                return sunBean;
            }

            sLog.warn("Process CPU usage isn't available from this JVM - the status bar won't show CPU usage");
        }
        catch(Throwable t)
        {
            sLog.error("Error accessing operating system MX bean to monitor CPU usage", t);
        }

        return null;
    }

    /**
     * Starts resource monitoring.  Does nothing when it's already running.
     */
    public synchronized void start()
    {
        if(mRunning)
        {
            return;
        }

        mRunning = true;
        mMemoryCpuMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::updateCpuMemory, 0, 1,
            TimeUnit.SECONDS);
        //The scheduled task only hands the scan to another thread, so a slow drive can't hold up the scheduler
        mStorageMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(this::scanStorage, 1,
            STORAGE_SCAN_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Sets the call database preference so that the recordings usage meter can use the size age-off rule limit.
     */
    public void setDatabaseSettings(DatabaseSettings databaseSettings)
    {
        mDatabaseSettings = databaseSettings;
    }

    /**
     * Stops resource monitoring.
     */
    public synchronized void stop()
    {
        mRunning = false;

        if(mMemoryCpuMonitorFuture != null)
        {
            mMemoryCpuMonitorFuture.cancel(true);
            mMemoryCpuMonitorFuture = null;
        }

        if(mStorageMonitorFuture != null)
        {
            mStorageMonitorFuture.cancel(true);
            mStorageMonitorFuture = null;
        }
    }

    /**
     * Indicates if monitoring is running.
     */
    public synchronized boolean isRunning()
    {
        return mRunning;
    }

    /**
     * Scans the storage folders again when the storage folders or limits change.
     */
    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.DIRECTORY && isRunning())
        {
            scanStorage();
        }
    }

    /**
     * Timer-driven method to update CPU and memory usage statistics.
     */
    private void updateCpuMemory()
    {
        //Negative when the value isn't available, which includes the first call on some platforms
        double processCpu = mOperatingSystemMXBean != null ? mOperatingSystemMXBean.getProcessCpuLoad() : -1;
        double systemCpu = mOperatingSystemMXBean != null ? mOperatingSystemMXBean.getCpuLoad() : -1;
        Runtime runtime = Runtime.getRuntime();
        long allocated = runtime.totalMemory();
        long used = allocated - runtime.freeMemory();
        long max = runtime.maxMemory();

        double cpuLevel;
        long memoryFloor;

        synchronized(mCpuSamples)
        {
            if(processCpu >= 0)
            {
                mCpuSamples.addLast(processCpu);

                while(mCpuSamples.size() > CPU_LEVEL_SAMPLES)
                {
                    mCpuSamples.removeFirst();
                }
            }

            cpuLevel = mCpuSamples.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            mMemorySamples.addLast(used);

            while(mMemorySamples.size() > MEMORY_LEVEL_SAMPLES)
            {
                mMemorySamples.removeFirst();
            }

            memoryFloor = mMemorySamples.stream().mapToLong(Long::longValue).min().orElse(used);
        }

        boolean cpuAvailable = mOperatingSystemMXBean != null;

        Platform.runLater(() -> {
            mMemoryMax.set(max);
            mMemoryAllocated.set(allocated);
            mMemoryUsed.set(used);
            mMemoryUsedPercentage.set(fraction(used, max));
            mMemoryLevelPercentage.set(fraction(memoryFloor, max));
            mCpuAvailable.set(cpuAvailable);

            if(processCpu >= 0)
            {
                mCpuPercentage.set(Math.min(processCpu, 1.0));
                mCpuLevelPercentage.set(Math.min(cpuLevel, 1.0));
            }

            mSystemCpuPercentage.set(systemCpu);
        });
    }

    /**
     * Scans the storage folders on a background thread, unless a scan is already running.
     */
    private void scanStorage()
    {
        if(mStorageScanRunning.compareAndSet(false, true))
        {
            ThreadPool.CACHED.submit(() -> {
                try
                {
                    updateStorageUsage();
                }
                finally
                {
                    mStorageScanRunning.set(false);
                }
            });
        }
    }

    /**
     * Measures the recordings and event logs folders.
     */
    private void updateStorageUsage()
    {
        long recordingsLimit = mSettings.getDirectorySettings().getDirectoryMaxUsageRecordings() * BYTES_PER_MEGABYTE;

        //When the call audio size age-off rule is enabled, the recordings meter shows usage against the rule's limit
        DatabaseSettings databaseSettings = mDatabaseSettings;

        if(databaseSettings != null && databaseSettings.isSizeRuleEnabled())
        {
            recordingsLimit = databaseSettings.getSizeRuleLimitMB() * BYTES_PER_MEGABYTE;
        }

        StorageUsage recordings = measure(mSettings.getDirectorySettings().getDirectoryRecording(), recordingsLimit);
        StorageUsage eventLogs = measure(mSettings.getDirectorySettings().getDirectoryEventLog(),
            mSettings.getDirectorySettings().getDirectoryMaxUsageEventLogs() * BYTES_PER_MEGABYTE);

        Platform.runLater(() -> {
            if(recordings != null)
            {
                mRecordingsUsage.set(recordings);
            }

            if(eventLogs != null)
            {
                mEventLogsUsage.set(eventLogs);
            }
        });
    }

    /**
     * Measures the space used by the folder.
     * @param folder to measure
     * @param limit set in the storage settings, in bytes
     * @return usage or null when the folder can't be measured
     */
    static StorageUsage measure(Path folder, long limit)
    {
        if(folder == null)
        {
            return null;
        }

        try
        {
            long free = Files.getFileStore(folder).getUsableSpace();
            long used = sizeOf(folder);
            return new StorageUsage(folder, used, limit, free);
        }
        catch(IOException ioe)
        {
            sLogSuppressor.error("Log Once", 1, "Unable to monitor file system - " + ioe.getMessage());
        }

        return null;
    }

    /**
     * Total size of the files in the folder and its sub-folders.  Files that are deleted or can't be read while the
     * folder is walked are skipped, since recordings and logs are deleted and written during the walk.
     */
    static long sizeOf(Path folder) throws IOException
    {
        long[] total = new long[1];

        Files.walkFileTree(folder, new SimpleFileVisitor<>()
        {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
            {
                if(attributes.isRegularFile())
                {
                    total[0] += attributes.size();
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exception)
            {
                return FileVisitResult.CONTINUE;
            }
        });

        return total[0];
    }

    private static double fraction(long value, long total)
    {
        return total > 0 ? (double)value / (double)total : 0.0;
    }

    /**
     * Process CPU usage across all processors, averaged over the last second.
     * @return usage in range 0.0 - 1.0
     */
    public DoubleProperty cpuPercentageProperty()
    {
        return mCpuPercentage;
    }

    /**
     * Process CPU usage averaged over the last few seconds, for warning levels.
     * @return usage in range 0.0 - 1.0
     */
    public DoubleProperty cpuLevelPercentageProperty()
    {
        return mCpuLevelPercentage;
    }

    /**
     * Whole system CPU usage across all processors.
     * @return usage in range 0.0 - 1.0, or negative when it isn't available
     */
    public DoubleProperty systemCpuPercentageProperty()
    {
        return mSystemCpuPercentage;
    }

    /**
     * Indicates if the process CPU usage is available from this JVM.
     */
    public BooleanProperty cpuAvailableProperty()
    {
        return mCpuAvailable;
    }

    /**
     * Maximum heap memory that the JVM can use (-Xmx).
     */
    public LongProperty memoryMaxProperty()
    {
        return mMemoryMax;
    }

    /**
     * Heap memory that the JVM has currently reserved from the operating system.
     */
    public LongProperty memoryAllocatedProperty()
    {
        return mMemoryAllocated;
    }

    /**
     * Heap memory in use, including garbage not yet collected.
     */
    public LongProperty memoryUsedProperty()
    {
        return mMemoryUsed;
    }

    /**
     * Heap memory used as a fraction of the maximum heap.
     * @return usage in range 0.0 - 1.0
     */
    public DoubleProperty memoryUsedPercentageProperty()
    {
        return mMemoryUsedPercentage;
    }

    /**
     * Lowest heap memory used over the last few seconds as a fraction of the maximum heap, which approximates the
     * memory still in use after garbage collection.  For warning levels.
     * @return usage in range 0.0 - 1.0
     */
    public DoubleProperty memoryLevelPercentageProperty()
    {
        return mMemoryLevelPercentage;
    }

    /**
     * Space used by the recordings folder, or null until the first scan.
     */
    public ObjectProperty<StorageUsage> recordingsUsageProperty()
    {
        return mRecordingsUsage;
    }

    /**
     * Space used by the event logs folder, or null until the first scan.
     */
    public ObjectProperty<StorageUsage> eventLogsUsageProperty()
    {
        return mEventLogsUsage;
    }

    /**
     * Space used by a storage folder.
     * @param folder that was measured
     * @param used bytes used by the files in the folder
     * @param limit bytes set as the folder's limit in the storage settings
     * @param free bytes still available on the folder's drive
     */
    public record StorageUsage(Path folder, long used, long limit, long free)
    {
        /**
         * Limit that applies: the storage setting, or less when the drive doesn't have that much space left.
         */
        public long effectiveLimit()
        {
            return Math.max(0, Math.min(limit, used + free));
        }

        /**
         * Used space as a fraction of the effective limit.
         * @return usage from 0.0, which can be larger than 1.0 when the folder exceeds its limit
         */
        public double fraction()
        {
            long effective = effectiveLimit();
            return effective > 0 ? (double)used / (double)effective : (used > 0 ? 1.0 : 0.0);
        }

        /**
         * Indicates if the drive, rather than the storage setting, sets the limit.
         */
        public boolean isDriveLimited()
        {
            return used + free < limit;
        }
    }
}
