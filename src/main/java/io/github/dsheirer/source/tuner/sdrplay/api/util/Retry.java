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

package io.github.dsheirer.source.tuner.sdrplay.api.util;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;

/**
 * Utility for retrying SDRplay API operations with thread sleep delay
 *
 * Note: the SDRplay API is non-blocking and therefore non-deterministic because it will return a Fail status code
 * if it can't execute the requested operation at that time.  Your only recourse is to repeatedly attempt an
 * operation until you get a successful status return code.
 */
public class Retry
{
    /**
     * Attempts to execute the runnable up to a maximum of 5 attempts, sleeping the thread by 20 millis each time.
     * @param r to execute
     */
    public static void quietly(Retryable r)
    {
        quietly(5, 20, r);
    }

    /**
     * Attempts to execute the runnable up to a maximum of (attempts), sleeping the thread by (pause) millis each time.
     * @param attempts number of tries
     * @param pause in milliseconds between each attempt
     * @param r to execute
     */
    public static void quietly(int attempts, long pause, Retryable r)
    {
        int attempt = 0;

        boolean success = false;

        while(attempt < attempts && !success)
        {
            attempt++;

            try
            {
                r.run();
                success = true;
            }
            catch(SDRPlayException e)
            {
                //Quietly ... no logging
            }

            if(!success)
            {
                try
                {
                    Thread.sleep(pause);
                }
                catch(Exception e)
                {
                    //Do nothing
                }
            }
        }
    }

    /**
     * Functional interface that encapsulates an SDRplay API operation that can throw an SDRplay exception and that
     * can be attempted multiple times until successful.
     */
    @FunctionalInterface
    public interface Retryable
    {
        void run() throws SDRPlayException;
    }
}
