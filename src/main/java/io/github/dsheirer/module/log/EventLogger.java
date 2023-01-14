/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.module.Module;
import io.github.dsheirer.util.TimeStamp;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EventLogger extends Module
{
    private final static Logger mLog = LoggerFactory.getLogger(EventLogger.class);

    private Path mLogDirectory;
    private String mFileNameSuffix;
    private String mLogFileName;
    private long mFrequency;
    protected Writer mLogFile;

    public EventLogger(Path logDirectory, String fileNameSuffix, long frequency)
    {
        mLogDirectory = logDirectory;
        mFileNameSuffix = fileNameSuffix;
        mFrequency = frequency;
    }

    public String toString()
    {
        if(mLogFileName != null)
        {
            return mLogFileName;
        }
        else
        {
            return "Unknown";
        }
    }

    public abstract String getHeader();

    @Override
    public void start()
    {
        if(mLogFile == null)
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mLogDirectory);
                sb.append(File.separator);
                sb.append(TimeStamp.getLongTimeStamp("_"));
                sb.append("_");
                sb.append(mFrequency);
                sb.append("_Hz_");
                sb.append(mFileNameSuffix);

                mLogFileName = sb.toString();
                mLogFile = new OutputStreamWriter(new FileOutputStream(mLogFileName));

                write(getHeader());
            }
            catch(FileNotFoundException e)
            {
                mLog.error("Couldn't create log file in directory:" + mLogDirectory);
            }
        }
    }

    public void stop()
    {
        if(mLogFile != null)
        {
            try
            {
                mLogFile.flush();
                mLogFile.close();
                mLogFile = null;
            }
            catch(Exception e)
            {
                mLog.error("Couldn't close log file:" + mFileNameSuffix);
            }
        }
    }

    protected void write(String eventLogEntry)
    {
        try
        {
            if(mLogFile != null)
            {
                mLogFile.write((eventLogEntry != null ? eventLogEntry : "") + "\n");
                mLogFile.flush();
            }
        }
        catch(Exception e)
        {
            mLog.error("Error writing entry to event log file", e);
        }
    }
}
