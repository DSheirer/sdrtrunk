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
 * State of the audio recording file for a call
 */
public enum RecordingState
{
    /**
     * Call is not recorded
     */
    NONE("Not Recorded"),
    /**
     * Recording is being written
     */
    RECORDING("Recording"),
    /**
     * Recording is complete and the file is available
     */
    RECORDED("Recorded"),
    /**
     * Recording file was deleted by an age-off rule or by the user
     */
    DELETED("Deleted"),
    /**
     * Recording failed with an error
     */
    FAILED("Failed");

    private final String mLabel;

    RecordingState(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Parses the stored value, defaulting to NONE for unrecognized values.
     */
    public static RecordingState fromValue(String value)
    {
        if(value != null)
        {
            try
            {
                return RecordingState.valueOf(value);
            }
            catch(IllegalArgumentException iae)
            {
                //Fall through
            }
        }

        return NONE;
    }
}
