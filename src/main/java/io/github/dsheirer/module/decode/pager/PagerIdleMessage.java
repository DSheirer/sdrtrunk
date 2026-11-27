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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * Paging protocol sync detection message.  Produced for each detected sync (a POCSAG batch sync codeword or a FLEX
 * frame sync) so that paging transmissions are visible in the message list, including transmissions that only carry
 * idle codewords.
 *
 * A POCSAG sync is only reported once the batch that follows it passes error correction, and a FLEX sync is only
 * reported once the frame information word passes error correction and its checksum, so that false sync detections
 * in noise are not reported.
 */
public class PagerIdleMessage extends Message
{
    private final Protocol mProtocol;
    private final int mBitRate;
    private final boolean mAfsk;
    private final String mDetails;

    /**
     * Constructs an instance
     * @param timestamp of the sync detection
     * @param protocol POCSAG or FLEX
     * @param bitRate of the transmission
     * @param afsk true if the transmission is AFSK audio tones instead of direct FSK
     * @param details protocol specific details, or null
     */
    public PagerIdleMessage(long timestamp, Protocol protocol, int bitRate, boolean afsk, String details)
    {
        super(timestamp);
        mProtocol = protocol;
        mBitRate = bitRate;
        mAfsk = afsk;
        mDetails = details;
    }

    @Override
    public Protocol getProtocol()
    {
        return mProtocol;
    }

    /**
     * Transmission bit rate.
     */
    public int getBitRate()
    {
        return mBitRate;
    }

    /**
     * Indicates if the transmission is AFSK audio tones over a voice channel instead of direct FSK.
     */
    public boolean isAfsk()
    {
        return mAfsk;
    }

    /**
     * Protocol specific details, or null.
     */
    public String getDetails()
    {
        return mDetails;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mProtocol).append(" ").append(mBitRate);

        if(mAfsk)
        {
            sb.append(" AFSK");
        }

        sb.append(" IDLE");

        if(mDetails != null && !mDetails.isEmpty())
        {
            sb.append(" ").append(mDetails);
        }

        return sb.toString();
    }
}
