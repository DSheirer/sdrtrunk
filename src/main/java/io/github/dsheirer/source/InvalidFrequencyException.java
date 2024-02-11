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
package io.github.dsheirer.source;

public class InvalidFrequencyException extends SourceException
{
    private long mInvalidFrequency;
    private long mValidFrequency;

    /**
     * Thrown when the user or the application requests to tune to an invalid frequency.  Invalid means that the
     * frequency is outside of the tunable range of the current tuner device.  This class provides both the requested
     * invalid frequency and the suggested valid frequency to use in lieu of the requested invalid frequency.
     * @param message optional text
     * @param invalidFrequency that was requested
     * @param validFrequency that should be used instead
     */
    public InvalidFrequencyException( String message, long invalidFrequency, long validFrequency )
    {
        super( message );

        mInvalidFrequency = invalidFrequency;
        mValidFrequency = validFrequency;
    }

    /**
     * The invalid requested frequency
     */
    public long getInvalidFrequency()
    {
        return mInvalidFrequency;
    }

    /**
     * The valid frequency that should be used
     */
    public long getValidFrequency()
    {
        return mValidFrequency;
    }
}
