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
package io.github.dsheirer.database.call;

/**
 * Call life-cycle state
 */
public enum CallState
{
    /**
     * Call is in progress
     */
    ACTIVE("Active"),
    /**
     * Call completed normally
     */
    COMPLETE("Complete"),
    /**
     * Call was in progress when sdrtrunk stopped (e.g. a crash) and never completed
     */
    INTERRUPTED("Interrupted");

    private final String mLabel;

    CallState(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Parses the stored value, defaulting to COMPLETE for unrecognized values.
     */
    public static CallState fromValue(String value)
    {
        if(value != null)
        {
            try
            {
                return CallState.valueOf(value);
            }
            catch(IllegalArgumentException iae)
            {
                //Fall through
            }
        }

        return COMPLETE;
    }
}
