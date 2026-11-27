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

package io.github.dsheirer.module.decode.pager;

/**
 * Paging message content types, common to all paging protocols.
 */
public enum PagerMessageType
{
    ALPHANUMERIC("Alphanumeric", "ALPHA"),
    NUMERIC("Numeric", "NUMERIC"),
    TONE("Tone Only", "TONE"),
    BINARY("Binary", "BINARY"),
    SECURE("Secure", "SECURE"),
    SHORT_INSTRUCTION("Short Instruction", "INSTRUCTION"),
    IDLE("Idle (Sync Detected)", "IDLE"),
    UNKNOWN("Unknown", "UNKNOWN");

    private final String mLabel;
    private final String mShortLabel;

    PagerMessageType(String label, String shortLabel)
    {
        mLabel = label;
        mShortLabel = shortLabel;
    }

    /**
     * Short label for message log display
     */
    public String getShortLabel()
    {
        return mShortLabel;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
