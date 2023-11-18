/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.util.ThreadPool;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides real-time monitoring of application resource usage.
 *
 * Monitors:
 * - CPU usage
 * - RAM usage
 * - Drive Space - Event Logs
 * - Drive Space - Recordings
 */
public class ResourceMonitor
{
    private static final Logger sLog = LoggerFactory.getLogger(ResourceMonitor.class);
    private static final LoggingSuppressor sLogSuppressor = new LoggingSuppressor(sLog);
    private static final int SCALOR_MEGABYTE = 1024 * 1024;
    private UserPreferences mUserPreferences;
    private ScheduledFuture<?> mMemoryCpuMonitorFuture;
    private ScheduledFuture<?> mStorageMonitorFuture;
    private LongProperty mMemoryTotal = new SimpleLongProperty();
    private LongProperty mMemoryAllocated = new SimpleLongProperty();
    private LongProperty mMemoryUsed = new SimpleLongProperty();
    private DoubleProperty mJavaMemoryUsedPercentage = new SimpleDoubleProperty();
    private DoubleProperty mSystemMemoryUsedPercentage = new SimpleDoubleProperty();
    private DoubleProperty mCpuPercentage = new SimpleDoubleProperty();
    private BooleanProperty mCpuAvailable = new SimpleBooleanProperty();
    private DoubleProperty mDirectoryUsePercentEventLogs = new SimpleDoubleProperty();
    private DoubleProperty mDirectoryUsePercentRecordings = new SimpleDoubleProperty();
    private StringProperty mFileSizeEventLogs = new SimpleStringProperty();
    private StringProperty mFileSizeRecordings = new SimpleStringProperty();
    private OperatingSystemMXBean mOperatingSystemMXBean;

    /**
     * Constructs an instance
     */
    public ResourceMonitor(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;

        try
        {
            mOperatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        }
        catch(Exception e)
        {
            sLog.error("Error accessing operating system MX bean to monitor CPU usage", e);
        }

        mMemoryTotal.set(Runtime.getRuntime().maxMemory());
    }

    /**
     * Starts resource monitoring.
     */
    public void start()
    {
        if(mMemoryCpuMonitorFuture == null)
        {
            mMemoryCpuMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> updateCpuMemory(), 1, 1, TimeUnit.SECONDS);
        }

        if(mStorageMonitorFuture == null)
        {
            mStorageMonitorFuture = ThreadPool.SCHEDULED.scheduleAtFixedRate(() -> updateDirectoryUsage(), 1,30, TimeUnit.SECONDS);
        }
    }

    /**
     * Stops resource monitoring.
     */
    public void stop()
    {
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
     * Timer-driven method to update CPU and memory usage statistics.
     */
    private void updateCpuMemory()
    {
        double cpuLoadScaled = 0.0;

        if(mOperatingSystemMXBean != null)
        {
            double load = mOperatingSystemMXBean.getSystemLoadAverage();
            cpuLoadScaled = load / mOperatingSystemMXBean.getAvailableProcessors();
        }

        final double loadFinal = cpuLoadScaled;

        Platform.runLater(() -> {
            mMemoryAllocated.set(Runtime.getRuntime().totalMemory());
            mMemoryUsed.set(mMemoryAllocated.getValue() - Runtime.getRuntime().freeMemory());
            mJavaMemoryUsedPercentage.set((double)mMemoryUsed.get() / (double)mMemoryAllocated.get());
            mSystemMemoryUsedPercentage.set((double)mMemoryAllocated.get() / (double)mMemoryTotal.get());
            mCpuPercentage.set(loadFinal > 0 ? loadFinal : 0);
            mCpuAvailable.set(loadFinal >= 0);
        });
    }

    /**
     * Timer-driven method to update directory space usage statistics
     */
    private void updateDirectoryUsage()
    {
        long thresholdEventLog = mUserPreferences.getDirectoryPreference().getDirectoryMaxUsageEventLogs() * SCALOR_MEGABYTE;
        long thresholdRecording = mUserPreferences.getDirectoryPreference().getDirectoryMaxUsageRecordings() * SCALOR_MEGABYTE;

        Path recordingPath = mUserPreferences.getDirectoryPreference().getDirectoryRecording();
        Path eventLogsPath = mUserPreferences.getDirectoryPreference().getDirectoryEventLog();

        try
        {
            FileStore recordingFileStore = Files.getFileStore(recordingPath);
            FileStore eventLogsFileStore = Files.getFileStore(eventLogsPath);

            long recordingAvailable = recordingFileStore.getUsableSpace();
            long eventLogsAvailable = eventLogsFileStore.getUsableSpace();

            long eventLogUsed = FileUtils.sizeOfDirectory(eventLogsPath.toFile());
            long recordingUsed = FileUtils.sizeOfDirectory(recordingPath.toFile());

            long eventLogMax = Math.min(thresholdEventLog, eventLogUsed + eventLogsAvailable);
            long recordingMax = Math.min(thresholdRecording, recordingUsed + recordingAvailable);

            Platform.runLater(() -> {
                mDirectoryUsePercentEventLogs.set((double)eventLogUsed / (double)eventLogMax);
                mDirectoryUsePercentRecordings.set((double)recordingUsed / (double)recordingMax);
                mFileSizeEventLogs.set(FileUtils.byteCountToDisplaySize(eventLogUsed));
                mFileSizeRecordings.set(FileUtils.byteCountToDisplaySize(recordingUsed));
            });
        }
        catch(IOException ioe)
        {
            sLogSuppressor.error("Log Once", 1, "Unable to monitor file system - " + ioe.getMessage());
        }
    }

    /**
     * Directory use percentage for event logs directory.
     * @return usage in range 0.0 - 1.0 (or larger if it exceeds the threshold)
     */
    public DoubleProperty directoryUsePercentEventLogsProperty()
    {
        return mDirectoryUsePercentEventLogs;
    }

    /**
     * Directory use percentage for recordings directory.
     * @return usage in range 0.0 - 1.0 (or larger if it exceeds the threshold)
     */
    public DoubleProperty directoryUsePercentRecordingsProperty()
    {
        return mDirectoryUsePercentRecordings;
    }

    /**
     * Formatted value property for file size in event logs directory.
     * @return print friendly value property
     */
    public StringProperty fileSizeEventLogsProperty()
    {
        return mFileSizeEventLogs;
    }

    /**
     * Formatted value property for file size in recordings directory.
     * @return print friendly value property
     */
    public StringProperty fileSizeRecordingsProperty()
    {
        return mFileSizeRecordings;
    }

    /**
     * CPU usage percentage.
     * @return usage in range 0.0 - 1.0
     */
    public DoubleProperty cpuPercentageProperty()
    {
        return mCpuPercentage;
    }

    /**
     * Indicates if the CPU usage percentage value is available on this operating system.
     * @return false when the CPU load value is a negative value, indicating that the JVM for this OS doesn't support it.
     */
    public BooleanProperty cpuAvailableProperty()
    {
        return mCpuAvailable;
    }

    /**
     * Property for total system memory
     */
    public LongProperty memoryTotalProperty()
    {
        return mMemoryTotal;
    }

    /**
     * Property for memory currently allocated to the JVM
     */
    public LongProperty memoryAllocatedProperty()
    {
        return mMemoryAllocated;
    }

    /**
     * Property for memory used by the JVM (out of what has been allocated)
     */
    public LongProperty memoryUsedProperty()
    {
        return mMemoryUsed;
    }

    /**
     * Property for memory used vs memory allocated to the JVM.
     */
    public DoubleProperty javaMemoryUsedPercentageProperty()
    {
        return mJavaMemoryUsedPercentage;
    }

    /**
     * Property for JVM allocated vs total system memory.
     */
    public DoubleProperty systemMemoryUsedPercentageProperty()
    {
        return mSystemMemoryUsedPercentage;
    }
}
