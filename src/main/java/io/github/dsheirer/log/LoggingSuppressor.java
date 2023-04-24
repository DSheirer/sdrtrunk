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

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;

/**
 * Logging suppressor that squelches log messages after they have been logged and continue to occur.
 */
public class LoggingSuppressor
{
    private Map<String,Integer> mSuppressionCountMap = new HashMap<>();
    private Logger mLogger;

    /**
     * Constructs an instance
     * @param logger to receive suppressed log messages.
     */
    public LoggingSuppressor(Logger logger)
    {
        mLogger = logger;
    }

    /**
     * Logs the information message.
     * @param key for suppression.
     * @param maxCount of the logged instance.
     * @param message to log.
     */
    public void info(String key, int maxCount, String message)
    {
        if(canLog(key, maxCount))
        {
            mLogger.info(message + getTag(key, maxCount));
        }
    }

    /**
     * Logs the information message.
     * @param key for suppression.
     * @param maxCount of the logged instance.
     * @param message to log.
     * @param t stack trace to include.
     */
    public void info(String key, int maxCount, String message, Throwable t)
    {
        if(canLog(key, maxCount))
        {
            mLogger.info(message + getTag(key, maxCount), t);
        }
    }

    /**
     * Logs the error message.
     * @param key for suppression.
     * @param maxCount of the logged instance.
     * @param message to log.
     */
    public void error(String key, int maxCount, String message)
    {
        if(canLog(key, maxCount))
        {
            mLogger.error(message + getTag(key, maxCount));
        }
    }

    /**
     * Logs the error message.
     * @param key for suppression.
     * @param maxCount of the logged instance.
     * @param message to log.
     * @param t stack trace to include.
     */
    public void error(String key, int maxCount, String message, Throwable t)
    {
        if(canLog(key, maxCount))
        {
            mLogger.error(message + getTag(key, maxCount), t);
        }
    }

    /**
     * Creates a logging tag that logs the suppression statistics.
     * @param key for retrieving current count.
     * @param maxCount to log.
     * @return logging tag
     */
    private String getTag(String key, int maxCount)
    {
        int count = mSuppressionCountMap.get(key);
        return " [Log Suppress " + count + "/" + maxCount + "]";
    }

    /**
     * Indicates if the operation can be logged without exceeding the max logging count specified by the user.
     * @param key for tracking by count
     * @param maxCount of the key to log
     * @return true if can log or false.
     */
    private boolean canLog(String key, int maxCount)
    {
        if(mSuppressionCountMap.containsKey(key))
        {
            int count = mSuppressionCountMap.get(key);

            if(count >= maxCount)
            {
                return false;
            }

            mSuppressionCountMap.put(key, count + 1);
        }
        else
        {
            mSuppressionCountMap.put(key, 1);
        }

        return true;
    }
}
