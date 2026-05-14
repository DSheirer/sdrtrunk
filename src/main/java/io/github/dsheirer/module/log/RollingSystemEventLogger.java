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
package io.github.dsheirer.module.log;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe rolling event logger that writes one file per day per system name.
 * Shared across all channels that belong to the same system.
 */
public class RollingSystemEventLogger
{
    private static final Logger mLog = LoggerFactory.getLogger(RollingSystemEventLogger.class);

    private final Path mLogDirectory;
    private final String mSystemName;
    private int mUserCount = 0;
    private LocalDate mFileDate;
    private BufferedWriter mWriter;

    /**
     * Constructs an instance.
     * @param logDirectory where log files are written
     * @param systemName sanitized system name used as filename prefix
     */
    public RollingSystemEventLogger(Path logDirectory, String systemName)
    {
        mLogDirectory = logDirectory;
        mSystemName = systemName;
    }

    /**
     * Increments the user count. Opens the file if this is the first user.
     */
    public synchronized void addUser()
    {
        mUserCount++;
        if(mUserCount == 1)
        {
            openFile();
        }
    }

    /**
     * Decrements the user count. Closes the file when no more users remain.
     */
    public synchronized void removeUser()
    {
        mUserCount--;
        if(mUserCount <= 0)
        {
            mUserCount = 0;
            closeFile();
        }
    }

    /**
     * Writes a CSV line to the log file, rolling over at midnight.
     * @param csvLine line to write (without trailing newline)
     */
    public synchronized void write(String csvLine)
    {
        LocalDate today = LocalDate.now();
        if(mWriter == null || !today.equals(mFileDate))
        {
            closeFile();
            openFile();
        }

        if(mWriter != null)
        {
            try
            {
                mWriter.write(csvLine);
                mWriter.write("\n");
                mWriter.flush();
            }
            catch(IOException e)
            {
                mLog.error("Error writing to system event log [" + mSystemName + "]", e);
            }
        }
    }

    /**
     * Opens the log file for today's date, writing header if newly created.
     */
    private void openFile()
    {
        mFileDate = LocalDate.now();
        String fileName = mSystemName + "_" + mFileDate + "_events.csv";
        Path logFile = mLogDirectory.resolve(fileName);

        try
        {
            boolean isNew = !Files.exists(logFile) || Files.size(logFile) == 0;
            mWriter = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            if(isNew)
            {
                mWriter.write(DecodeEventLogger.getCSVHeader());
                mWriter.write("\n");
                mWriter.flush();
            }
        }
        catch(IOException e)
        {
            mLog.error("Error opening system event log file [" + logFile + "]", e);
            mWriter = null;
        }
    }

    /**
     * Closes the current log file.
     */
    private void closeFile()
    {
        if(mWriter != null)
        {
            try
            {
                mWriter.flush();
                mWriter.close();
            }
            catch(IOException e)
            {
                mLog.error("Error closing system event log [" + mSystemName + "]", e);
            }
            finally
            {
                mWriter = null;
            }
        }
    }
}
