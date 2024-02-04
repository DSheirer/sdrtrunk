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

package io.github.dsheirer.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logback and SLF4j logging implementation.
 */
public class ApplicationLog
{
    private final static Logger mLog = LoggerFactory.getLogger(ApplicationLog.class);

    private static final String APPLICATION_LOG_FILENAME = "sdrtrunk_app.log";
    private static final int APPLICATION_LOG_MAX_HISTORY = 10;

    private UserPreferences mUserPreferences;
    private RollingFileAppender mRollingFileAppender;
    private Path mApplicationLogPath;

    /**
     * Constructs the application log instance.  Note: use the start() method to initiate logging.
     * @param userPreferences
     */
    public ApplicationLog(UserPreferences userPreferences)
    {
        mUserPreferences = userPreferences;
    }

    @Subscribe
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DIRECTORY && mRollingFileAppender != null && mApplicationLogPath != null)
        {
            Path applicationLogPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();

            //Restart the application log if the path is updated
            if(!applicationLogPath.equals(mApplicationLogPath))
            {
                mLog.info("Application logging directory has changed [" + applicationLogPath.toString() + " ] - restarting logging");
                stop();
                start();
            }
        }
    }

    /**
     * Starts application logging
     */
    public void start()
    {
        MyEventBus.getGlobalEventBus().register(this);

        if(mRollingFileAppender == null)
        {
            mApplicationLogPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logfile = mApplicationLogPath.resolve(APPLICATION_LOG_FILENAME);
            mLog.info("Application Log File: " + logfile.toString());

            LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();

            PatternLayoutEncoder encoder = new PatternLayoutEncoder();
            encoder.setContext(loggerContext);
            encoder.setPattern("%-25(%d{yyyyMMdd HHmmss.SSS} [%thread]) %-5level %logger{30} - %msg  %memory_usage%n");
            encoder.start();

            mRollingFileAppender = new RollingFileAppender();
            mRollingFileAppender.setContext(loggerContext);
            mRollingFileAppender.setAppend(true);
            mRollingFileAppender.setName("FILE");
            mRollingFileAppender.setEncoder(encoder);
            mRollingFileAppender.setFile(logfile.toString());

            ThresholdFilter thresholdFilter = new ThresholdFilter();
            thresholdFilter.setLevel(Level.DEBUG.toString());
            thresholdFilter.setContext(loggerContext);
            thresholdFilter.setName("sdrtrunk threshold filter");
            thresholdFilter.start();

            mRollingFileAppender.addFilter(thresholdFilter);

            String pattern = logfile.toString().replace(APPLICATION_LOG_FILENAME, "%d{yyyyMMdd}_" + APPLICATION_LOG_FILENAME);
            TimeBasedRollingPolicy rollingPolicy = new TimeBasedRollingPolicy<>();
            rollingPolicy.setContext(loggerContext);
            rollingPolicy.setFileNamePattern(pattern);
            rollingPolicy.setMaxHistory(APPLICATION_LOG_MAX_HISTORY);
            rollingPolicy.setParent(mRollingFileAppender);
            rollingPolicy.start();

            mRollingFileAppender.setRollingPolicy(rollingPolicy);
            mRollingFileAppender.start();

            Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            ((ch.qos.logback.classic.Logger)logger).setLevel(Level.ALL);
            ((ch.qos.logback.classic.Logger)logger).addAppender(mRollingFileAppender);

//            StatusPrinter.print(loggerContext);
            Attributes atts = findManifestAttributes();
            if (atts != null) {
                mLog.info("SDRTrunk Version  : " + atts.getValue("Implementation-Version"));
                mLog.info("Gradle Version    : " + atts.getValue("Created-By"));
                mLog.info("Build Timestamp   : " + atts.getValue("Build-Timestamp"));
                mLog.info("Build-JDK         : " + atts.getValue("Build-JDK"));
                mLog.info("Build OS          : " + atts.getValue("Build-OS"));
            }
            else{
                mLog.info("Failed to find build information.");
            }

            mLog.info("");
            mLog.info("*******************************************************************");
            mLog.info("**** sdrtrunk: a trunked radio and digital decoding application ***");
            mLog.info("****  website: https://github.com/dsheirer/sdrtrunk             ***");
            mLog.info("*******************************************************************");
            mLog.info("Memory Logging Format: [Used/Allocated PercentUsed%]");
            mLog.info("Host OS Name:          " + System.getProperty("os.name"));
            mLog.info("Host OS Arch:          " + System.getProperty("os.arch"));
            mLog.info("Host OS Version:       " + System.getProperty("os.version"));
            mLog.info("Host CPU Cores:        " + Runtime.getRuntime().availableProcessors());
            mLog.info("Host Max Java Memory:  " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()));
            mLog.info("Storage Directories:");
            mLog.info(" Application Root: " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot());
            mLog.info(" Application Log:  " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog());
            mLog.info(" Event Log:        " + mUserPreferences.getDirectoryPreference().getDirectoryEventLog());
            mLog.info(" Playlist:         " + mUserPreferences.getDirectoryPreference().getDirectoryPlaylist());
            mLog.info(" Recordings:       " + mUserPreferences.getDirectoryPreference().getDirectoryRecording());
        }

    }

    /**
     * Stops the log file appender and nullifies it for shutdown or for reinitialization.
     */
    public void stop()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
        if(mRollingFileAppender != null)
        {
            mLog.info("Stopping application logging");
            mRollingFileAppender.stop();
            LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
            Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            ((ch.qos.logback.classic.Logger)logger).detachAppender(mRollingFileAppender);
            mRollingFileAppender = null;
        }
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
}
