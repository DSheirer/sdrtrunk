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

package io.github.dsheirer.gui.playlist.radioreference;

/**
 * Enumeration of radio reference talkgroup encryption status
 */
public enum TalkgroupEncryption
{
    UNENCRYPTED("No Encryption", 0),
    PARTIAL("Partial Encrypted", 1),
    FULL("Encrypted", 2),
    UNKNOWN("Unknown", -1);

    private String mLabel;
    private int mValue;

    TalkgroupEncryption(String label, int value)
    {
        mLabel = label;
        mValue = value;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    public static TalkgroupEncryption lookup(int value)
    {
        switch(value)
        {
            case 0:
                return UNENCRYPTED;
            case 1:
                return PARTIAL;
            case 2:
                return FULL;
            default:
                return UNKNOWN;
        }
    }
}
