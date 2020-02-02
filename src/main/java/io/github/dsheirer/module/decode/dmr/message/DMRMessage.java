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

package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base DMR Message
 */
public abstract class DMRMessage extends Message
{
    private CorrectedBinaryMessage mCorrectedBinaryMessage;
    private boolean mValid = true;
    private int mTimeslot;

    /**
     * Constructs an instance
     * @param timestamp for the message
     */
    public DMRMessage(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(timestamp);
        mCorrectedBinaryMessage = message;
        mTimeslot = timeslot;
    }

    /**
     * Message bits
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mCorrectedBinaryMessage;
    }

    /**
     * Indicates if this message is valid
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the valid flag for this message
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    /**
     * Timeslot for this message
     * @return 0 or 1
     */
    public int getTimeslot()
    {
        return mTimeslot;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }
}
