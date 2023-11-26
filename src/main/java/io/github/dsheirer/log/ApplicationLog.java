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

package io.github.dsheirer.log;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.filter.ThresholdFilter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import io.github.dsheirer.preference.IPreferenceUpdateListener;
import io.github.dsheirer.preference.PreferenceType;
import io.github.dsheirer.preference.UserPreferences;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * Logback and SLF4j logging implementation.
 */
@Component("applicationLog")
public class ApplicationLog implements IPreferenceUpdateListener, DisposableBean
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ApplicationLog.class);
    private static final String APPLICATION_LOG_FILENAME = "sdrtrunk_app.log";
    private static final int APPLICATION_LOG_MAX_HISTORY = 10;
    @Resource
    private UserPreferences mUserPreferences;
    private RollingFileAppender mRollingFileAppender;
    private Path mApplicationLogPath;

    /**
     * Constructs the application log instance.  Note: use the start() method to initiate logging.
     */
    public ApplicationLog()
    {
    }

    @Override
    public void preferenceUpdated(PreferenceType preferenceType)
    {
        if(preferenceType == PreferenceType.DIRECTORY && mRollingFileAppender != null && mApplicationLogPath != null)
        {
            Path applicationLogPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();

            //Restart the application log if the path is updated
            if(!applicationLogPath.equals(mApplicationLogPath))
            {
                LOGGER.info("Application logging directory has changed [" + applicationLogPath.toString() + " ] - restarting logging");
                stop();
                start();
            }
        }
    }

    /**
     * Starts application logging
     */
    @PostConstruct
    public void start()
    {
        mUserPreferences.addUpdateListener(this);


        if(mRollingFileAppender == null)
        {
            mApplicationLogPath = mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog();
            Path logfile = mApplicationLogPath.resolve(APPLICATION_LOG_FILENAME);

            LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            Appender appender = rootLogger.getAppender("IN-MEMORY");

            List<ILoggingEvent> cachedLogEvents = new ArrayList<>();
            if(appender instanceof CachingLogAppender cachingLogAppender)
            {
                cachedLogEvents.addAll(cachingLogAppender.disable());
                rootLogger.detachAppender("IN-MEMORY");
            }

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

            LOGGER.info("Memory Logging Format: [Used/Allocated AllocatedUsage%]");
            LOGGER.info("Application Log File: " + logfile);
            LOGGER.info("*******************************************************************");
            LOGGER.info("**** sdrtrunk: a trunked radio and digital decoding application ***");
            LOGGER.info("****  website: https://github.com/dsheirer/sdrtrunk             ***");
            LOGGER.info("*******************************************************************");

            Attributes atts = findManifestAttributes();
            if (atts != null)
            {
                LOGGER.info("SDRTrunk Version  : " + atts.getValue("Implementation-Version"));
                LOGGER.info("Gradle Version    : " + atts.getValue("Created-By"));
                LOGGER.info("Build Timestamp   : " + atts.getValue("Build-Timestamp"));
                LOGGER.info("Build-JDK         : " + atts.getValue("Build-JDK"));
                LOGGER.info("Build OS          : " + atts.getValue("Build-OS"));
            }
            else
            {
                LOGGER.info("Failed to find build information.");
            }

            LOGGER.info("Host OS Name:          " + System.getProperty("os.name"));
            LOGGER.info("Host OS Arch:          " + System.getProperty("os.arch"));
            LOGGER.info("Host OS Version:       " + System.getProperty("os.version"));
            LOGGER.info("Host CPU Cores:        " + Runtime.getRuntime().availableProcessors());
            LOGGER.info("Host Max Java Memory:  " + FileUtils.byteCountToDisplaySize(Runtime.getRuntime().maxMemory()));
            LOGGER.info("Storage Directories:");
            LOGGER.info(" Application Root: " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot());
            LOGGER.info(" Application Log:  " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationLog());
            LOGGER.info(" Event Log:        " + mUserPreferences.getDirectoryPreference().getDirectoryEventLog());
            LOGGER.info(" Icons/Settings:   " + mUserPreferences.getDirectoryPreference().getDirectoryApplicationRoot().resolve("settings"));
            LOGGER.info(" Playlist:         " + mUserPreferences.getDirectoryPreference().getDirectoryPlaylist());
            LOGGER.info(" Recordings:       " + mUserPreferences.getDirectoryPreference().getDirectoryRecording());

            for(ILoggingEvent loggingEvent: cachedLogEvents)
            {
                LOGGER.info("[Cached Log] " + loggingEvent.getFormattedMessage());
            }
        }
    }

    @Override
    public void destroy() throws Exception
    {
        stop();
    }

    /**
     * Stops the log file appender and nullifies it for shutdown or for reinitialization.
     */
    public void stop()
    {
        mUserPreferences.removeUpdateListener(this);
        if(mRollingFileAppender != null)
        {
            LOGGER.info("Stopping application logging");
            mRollingFileAppender.stop();
            LoggerContext loggerContext = (LoggerContext)LoggerFactory.getILoggerFactory();
            Logger logger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            ((ch.qos.logback.classic.Logger)logger).detachAppender(mRollingFileAppender);
            mRollingFileAppender = null;
        }
    }

    /**
     * Retrieves the jar manifest attributes
     * @return attributes or null.
     */
    private Attributes findManifestAttributes() {
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
        } catch (Exception ex) {
            return null;
        }
        return null;
    }
}
