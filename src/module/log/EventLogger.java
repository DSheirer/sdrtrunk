/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package module.log;

import module.Module;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.TimeStamp;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.concurrent.ScheduledExecutorService;

public abstract class EventLogger extends Module
{
    private final static Logger mLog = LoggerFactory.getLogger(EventLogger.class);

    private static final String[] ILLEGAL_FILENAME_CHARACTERS = {"#", "%", "&", "{", "}", "\\", "<", ">",
        "*", "?", "/", " ", "$", "!", "'", "\"", ":", "@", "+", "`", "|", "="};

    private Path mLogDirectory;
    private String mFileNameSuffix;
    private String mLogFileName;
    protected Writer mLogFile;

    public EventLogger(Path logDirectory, String fileNameSuffix)
    {
        mLogDirectory = logDirectory;
        mFileNameSuffix = fileNameSuffix;
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
    public void start(ScheduledExecutorService executor)
    {
        if(mLogFile == null)
        {
            try
            {
                StringBuilder sb = new StringBuilder();
                sb.append(mLogDirectory);
                sb.append(File.separator);
                sb.append(TimeStamp.getTimeStamp("_"));
                sb.append("_");
                sb.append(replaceIllegalCharacters(mFileNameSuffix));

                mLogFileName = sb.toString();

                mLog.info("Creating log file:" + mLogFileName);

                mLogFile = new OutputStreamWriter(new FileOutputStream(mLogFileName));

                write(getHeader());
            }
            catch(FileNotFoundException e)
            {
                mLog.error("Couldn't create log file in directory:" + mLogDirectory);
            }
        }
    }

    /**
     * Replaces any illegal filename characters in the proposed filename
     */
    private String replaceIllegalCharacters(String filename)
    {
        for(String illegalCharacter : ILLEGAL_FILENAME_CHARACTERS)
        {
            filename = filename.replace(illegalCharacter, "_");
        }

        return filename;
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
            mLogFile.write(eventLogEntry + "\n");
            mLogFile.flush();
        }
        catch(IOException e)
        {
            mLog.error("Error writing entry to event log file", e);
        }
    }
}
