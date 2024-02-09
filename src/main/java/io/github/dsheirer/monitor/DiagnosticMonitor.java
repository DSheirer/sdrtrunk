/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.log.LoggingSuppressor;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import io.github.dsheirer.util.TimeStamp;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;

/**
 * Utility class for monitoring system components and producing logging reports.
 */
public class DiagnosticMonitor
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DiagnosticMonitor.class);
    private final LoggingSuppressor LOG_SUPPRESSOR = new LoggingSuppressor(LOGGER);
    private static final String DIVIDER = "\n\n=========================================================================\n\n";
    private UserPreferences mUserPreferences;
    private ChannelProcessingManager mChannelProcessingManager;
    private TunerManager mTunerManager;
    private ScheduledFuture<?> mBlockedThreadMonitorHandle;
    private BlockedThreadMonitor mMonitor = new BlockedThreadMonitor();
    private boolean mUserAlertedToBlockedThreadCondition = false;
    private Map<Integer,Integer> mBlockedThreadDetectionCountMap = new HashMap<>();
    private boolean mHeadless;

    /**
     * Constructs an instance
     * @param userPreferences for application logging directory lookup.
     * @param channelProcessingManager for accessing running channel information
     * @param tunerManager for accessing allocated tuner channel information
     * @param headless to indicate if the thread deadlock monitor should show a user notification.
     */
    public DiagnosticMonitor(UserPreferences userPreferences, ChannelProcessingManager channelProcessingManager,
                             TunerManager tunerManager, boolean headless)
    {
        mUserPreferences = userPreferences;
        mChannelProcessingManager = channelProcessingManager;
        mTunerManager = tunerManager;
        mHeadless = headless;
    }

    /**
     * Starts monitoring for blocked threads
     */
    public void start()
    {
        if(mBlockedThreadMonitorHandle != null)
        {
            mBlockedThreadMonitorHandle.cancel(true);
        }

        if(mUserPreferences.getApplicationPreference().isAutomaticDiagnosticMonitoring())
        {
            LOGGER.info("Diagnostic monitoring enabled running every 30 seconds");
            mBlockedThreadMonitorHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(mMonitor, 30, 30, TimeUnit.SECONDS);
        }
        else
        {
            LOGGER.info("Diagnostic monitoring disabled per user preference (application).");
        }
    }

    /**
     * Stops monitoring for blocked threads.
     */
    public void stop()
    {
        if(mBlockedThreadMonitorHandle != null)
        {
            mBlockedThreadMonitorHandle.cancel(true);
        }

        mBlockedThreadMonitorHandle = null;
    }

    /**
     * Checks for blocked threads and on discovery, generates a diagnostic report and notifies the user (once).
     */
    private void checkForBlockedThreads()
    {
        if(!mUserAlertedToBlockedThreadCondition)
        {
            try
            {
                ThreadMXBean bean = ManagementFactory.getThreadMXBean();

                long ids[] = bean.findDeadlockedThreads();

                if(ids != null)
                {
                    mUserAlertedToBlockedThreadCondition = true;

                    ThreadInfo threadInfo[] = bean.getThreadInfo(ids);

                    StringBuilder sb = new StringBuilder();
                    sb.append("sdrtrunk detected a critical application error with a threading deadlock, described as follows:\n");

                    for (ThreadInfo threadInfo1 : threadInfo)
                    {
                        sb.append("Thread ID[").append(threadInfo1.getThreadId());
                        sb.append("] Name [").append(threadInfo1.getThreadName());
                        sb.append("] Lock [").append(threadInfo1.getLockName());
                        sb.append("] Owned By [ID:").append(threadInfo1.getLockOwnerId());
                        sb.append(" | NAME:").append(threadInfo1.getLockName());
                        sb.append("]\n");
                    }

                    LOGGER.error(sb.toString());
                    Path reportPath = generateProcessingDiagnosticReport(sb + DIVIDER);
                    LOGGER.error("Thread deadlock report generated: " + reportPath);

                    if(!mHeadless)
                    {
                        String title = "sdrtrunk: Critical Error Detected";
                        String message = "The sdrtrunk application has detected a thread deadlock situation.\n" +
                                         "The application may degrade over time and eventually run out of memory.\n" +
                                         "A diagnostic report was generated.  Please open an issue on the GitHub\n" +
                                         "website and attach this diagnostic report:\n\n" + reportPath.toString();
                        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
            catch(Exception e)
            {
                LOG_SUPPRESSOR.error("run error", 1, "Error while monitoring for deadlocked " +
                        "threads: " + e.getLocalizedMessage());
                //Set the flag so that we don't try to run again.
                mUserAlertedToBlockedThreadCondition = true;
            }
        }
    }

    /**
     * Creates a diagnostic report containing state information for channels that are in a processing state.
     * @param message to prepend to the report
     * @return path for the log file that was created.
     */
    public Path generateProcessingDiagnosticReport(String message) throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append(message);
        sb.append("\n\nsdrtrunk Processing Diagnostic Report\n");
        sb.append(DIVIDER);
        sb.append(getEnvironmentReport());
        sb.append(DIVIDER);
        sb.append(mTunerManager.getDiscoveredTunerModel().getDiagnosticReport());
        sb.append(DIVIDER);
        sb.append(mChannelProcessingManager.getDiagnosticInformation());
        sb.append(DIVIDER);
        sb.append(mChannelProcessingManager.getChannelMetadataModel().getDiagnosticInformation());
        sb.append(DIVIDER);
        sb.append(getThreadDumpReport());
        sb.append(DIVIDER);

        Path logDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
        String file = TimeStamp.getFileFormattedDateTime() + "_sdrtrunk_processing_diagnostic_report.log";
        Path output = logDirectory.resolve(file);
        Files.write(output, sb.toString().getBytes());
        return output;
    }

    /**
     * Dumps the current threads to a log file with current date and time to the application log directory.
     * @return path to the thread dump log file that was created.
     * @throws IOException if there is an issue writing the contents to the log file.
     */
    public Path generateThreadDumpReport() throws IOException
    {
        String report = getThreadDumpReport();
        Path logDirectory = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
        String file = TimeStamp.getFileFormattedDateTime() + "_sdrtrunk_thread_dump.log";
        Path output = logDirectory.resolve(file);
        Files.write(output, report.getBytes());
        return output;
    }

    /**
     * Creates a thread dump report.
     * @return report text.
     */
    public String getThreadDumpReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Thread Dump Report\n\n");

        for(Map.Entry<Thread, StackTraceElement[]> entry : Thread.getAllStackTraces().entrySet())
        {
            sb.append(entry.getKey() + " " + entry.getKey().getState()).append("\n");

            for(StackTraceElement ste : entry.getValue())
            {
                sb.append("\tat " + ste).append("\n");
            }

            sb.append("\n\n");
        }

        return sb.toString();
    }

    /**
     * Generates a JVM and application environment report
     */
    public String getEnvironmentReport()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("JVM and Application Environment Report\n");
        Attributes atts = findManifestAttributes();
        if (atts != null) {
            sb.append("\nVersion  : " + atts.getValue("Implementation-Version"));
            sb.append("\nGradle Version    : " + atts.getValue("Created-By"));
            sb.append("\nBuild Timestamp   : " + atts.getValue("Build-Timestamp"));
            sb.append("\nBuild-JDK         : " + atts.getValue("Build-JDK"));
            sb.append("\nBuild OS          : " + atts.getValue("Build-OS"));
        }
        else
        {
            sb.append("\nApplication:       no build information available");
        }

        sb.append("\nHost OS Name:          " + System.getProperty("os.name"));
        sb.append("\nHost OS Arch:          " + System.getProperty("os.arch"));
        sb.append("\nHost OS Version:       " + System.getProperty("os.version"));
        sb.append("\nHost CPU Cores:        " + Runtime.getRuntime().availableProcessors());
        sb.append("\nHost Max Java Memory:  " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()));
        sb.append("\nHost Allocated Memory: " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory()));
        sb.append("\nHost Free Memory:      " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().freeMemory()));
        sb.append("\nHost Used Memory:      " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().totalMemory() -
                Runtime.getRuntime().freeMemory()));
        sb.append("\nStorage Directories:");
        sb.append("\n Application Root: " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot());
        sb.append("\n Application Log:  " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog());
        sb.append("\n Event Log:        " + mUserPreferences.getDirectoryPreference().getDirectoryEventLog());
        sb.append("\n Playlist:         " + mUserPreferences.getDirectoryPreference().getDirectoryPlaylist());
        sb.append("\n Recordings:       " + mUserPreferences.getDirectoryPreference().getDirectoryRecording());
        sb.append("\n");
        return sb.toString();
    }

    /**
     * Finds the jar manifest attributes
     * @return attributes or null.
     */
    public Attributes findManifestAttributes() {
        try {
            Enumeration<URL> resources = getClass().getClassLoader().getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                try {
                    Manifest manifest = new Manifest(resources.nextElement().openStream());
                    Attributes atts = manifest.getMainAttributes();
                    Boolean hasTitle = atts.containsValue("sdrtrunk project");
                    if (hasTitle) {
                        return atts;
                    }
                } catch (IOException E) {
                    return null;
                }
            }
        }
        catch (Exception ex)
        {
            return null;
        }

        return null;
    }

    /**
     * Runnable to periodically check for blocked threads
     */
    public class BlockedThreadMonitor implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
                checkForBlockedThreads();
            }
            catch(Throwable t)
            {
                LOG_SUPPRESSOR.error("Error", 3, "Error while checking for blocked threads", t);
            }
        }
    }
}
