/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.message;

import io.github.dsheirer.identifier.Identifier;

import java.util.Collections;
import java.util.List;

/**
 * Empty message to convey that a number of bits were processed with no sync or valid message detected.
 *
 * This message supports periodic updates to external processor(s) that may be tracking bit error rates.
 */
public class SyncLossMessage extends Message
{
    private int mBitsProcessed;

    /**
     * Constructs a sync loss message.
     *
     * @param timestamp of the message
     * @param bitsProcessed without a sync pattern detection of message decode
     */
    public SyncLossMessage(long timestamp, int bitsProcessed)
    {
        super(timestamp);
        mBitsProcessed = bitsProcessed;
    }

    /**
     * Indicates the number of bits that were processed without a sync dectection or message decode
     */
    public int getBitsProcessed()
    {
        return mBitsProcessed;
    }

    @Override
    public String toString()
    {
        return "<-> SYNC LOSS - BITS PROCESSED [" + getBitsProcessed() + "]";
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public String getProtocol()
    {
        return "NONE";
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
