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

package io.github.dsheirer.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * Timeslot message base class.
 */
public abstract class TimeslotMessage extends AbstractMessage implements IMessage
{
    public static final int TIMESLOT_0 = 0;
    public static final int TIMESLOT_1 = 1;
    public static final int TIMESLOT_2 = 2;
    private long mTimestamp;
    private int mTimeslot;
    private boolean mValid = true;

    /**
     * Constructs an instance
     * @param message containing binary fields that may contain fields outside of the scope of this class and thus
     * requires an offset value that points to the start of this message structure within the larger message context.
     * @param offset in the binary message to the start of this chunk of the message.
     * @param timeslot for this message
     * @param timestamp for this message
     */
    public TimeslotMessage(CorrectedBinaryMessage message, int offset, int timeslot, long timestamp)
    {
        super(message, offset);
        mTimeslot = timeslot;
        mTimestamp = timestamp;
    }

    /**
     * Constructs an instance
     * @param message containing binary fields.
     * @param timeslot for this message
     * @param timestamp for this message
     */
    public TimeslotMessage(CorrectedBinaryMessage message, int timeslot, long timestamp)
    {
        super(message);
        mTimeslot = timeslot;
        mTimestamp = timestamp;
    }

    /**
     * Timeslot for this message.
     * @return timeslot, default of 0 for no timeslot.
     */
    public int getTimeslot()
    {
        return mTimeslot;
    }

    /**
     * Timestamp when the message was received or processed.  This timestamp should be as close to
     * accurate as possible.
     */
    public long getTimestamp()
    {
        return mTimestamp;
    }

    /**
     * Decoded textual representation of the message
     */
    public abstract String toString();

    /**
     * Indicates if the message is valid and has passed crc/integrity checks
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the validity flag to indicate if the message is valid and passed any CRC or integrity checks.
     * @param valid flag
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }
}
