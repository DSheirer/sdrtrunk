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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

/**
 * Used by Cap+ channel status message to indicate if the channel status payload is a single fragment, or a
 * multi-sequence fragment.
 */
public enum SegmentIndicator
{
    /**
     * Continuation Segment
     */
    CONTINUATION_SEGMENT("[--]"),

    /**
     * Last Segment in a multi-segment sequence
     */
    LAST_SEGMENT("[-L]"),

    /**
     * First Segment in a multi-segment sequence
     */
    FIRST_SEGMENT("[F-]"),

    /**
     * Single Segment.
     */
    SINGLE_SEGMENT("[FL]"),

    /**
     * Unknow Segment
     */
    UNKNOWN("[**]");

    private String mLabel;

    /**
     * Constructs an instance
     * @param label to display
     */
    SegmentIndicator(String label)
    {
        mLabel = label;
    }

    /**
     * Lookup a segment indicator from the value.
     * @param value to lookup
     * @return segment indicator (0-3) or UNKNOWN.
     */
    public static SegmentIndicator fromValue(int value)
    {
        if(0 <= value && value < 4)
        {
            return SegmentIndicator.values()[value];
        }

        return UNKNOWN;
    }

    /**
     * Indicates if this is the first in a multi-segment sequence or if this is a single-segment.
     */
    public boolean isFirst()
    {
        return this.equals(FIRST_SEGMENT) || this.equals(SINGLE_SEGMENT);
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}