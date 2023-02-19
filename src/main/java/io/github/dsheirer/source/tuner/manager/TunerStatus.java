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

package io.github.dsheirer.source.tuner.manager;

/**
 * Current status of a discovered tuner.
 */
public enum TunerStatus
{
    /**
     * A tuner that is enabled and usable, but not currently in-use.
     */
    ENABLED("Enabled"),

    /**
     * A tuner that has been disabled or black-listed for use
     */
    DISABLED("Disabled"),


    /**
     * Indicates that the tuner has been removed from the system.
      */
    REMOVED("Removed"),

    /**
     * A tuner that has an error state and is unusable
     */
    ERROR("Error");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label
     */
    TunerStatus(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Indicates if this tuner is available and can be started
     */
    public boolean isAvailable()
    {
        return this == ENABLED;
    }
}
