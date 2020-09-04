/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.gui.preference;

/**
 * Preference editor tree node enumeration.
 */
public enum PreferenceEditorType
{
    CHANNEL_EVENT("Channel Events"),
    DIRECTORY("Directories"),
    JMBE_LIBRARY("JMBE Audio Library"),
    AUDIO_RECORD("Record"),
    AUDIO_OUTPUT("Output/Tones"),
    AUDIO_DUPLICATE_CALL_DETECTION("Duplicate Calls"),
    SOURCE_TUNER_CHANNELIZER("Tuner Channelizer"),
    TALKGROUP_FORMAT("Talkgroup & Radio ID"),
    DEFAULT("Default");

    private String mLabel;

    PreferenceEditorType(String label)
    {
        mLabel = label;
    }

    public String toString()
    {
        return mLabel;
    }
}
