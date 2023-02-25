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

package io.github.dsheirer.module.decode.ip.mototrbo.xcmp;

import com.google.common.base.Joiner;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.ip.IHeader;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * XCMP Transfer: Network Frequency File
 */
public class NetworkFrequencyFile extends Packet
{
    private static final int[] VERSION = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SEGMENT_NUMBER = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] SEGMENT_COUNT = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] NETWORK = new int[]{48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59};
    private static final int[] UNKNOWN = new int[]{60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76,
        77, 78, 79};
    private static final int REPEATER_STRUCTURE_START = 80;
    private static final int REPEATER_STRUCTURE_LENGTH = 72;
    private List<Repeater> mRepeaters;
    private DMRNetwork mNetwork;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public NetworkFrequencyFile(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NETWORK FREQUENCY FILE VERSION:").append(getVersion());
        sb.append(" NETWORK:").append(getNetwork());
        sb.append(" SEGMENT:").append(getSegmentNumber()).append(" OF ").append(getSegmentCount());

        List<Repeater> repeaters = getRepeaters();

        if(repeaters.isEmpty())
        {
            sb.append(" NO REPEATER INFO");
        }
        else
        {
            sb.append(" ").append(Joiner.on(" | ").join(mRepeaters));
        }

        return sb.toString();
    }

    public DMRNetwork getNetwork()
    {
        if(mNetwork == null)
        {
            mNetwork = DMRNetwork.create(getMessage().getInt(NETWORK, getOffset()));
        }

        return mNetwork;
    }

    /**
     * Unknown 20-bit field contents
     */
    public String getUnknown()
    {
        return String.format("%05X", getMessage().getInt(UNKNOWN, getOffset()));
    }

    /**
     * Segment number for this packet
     *
     * @return
     */
    public int getSegmentNumber()
    {
        return getMessage().getInt(SEGMENT_NUMBER, getOffset());
    }

    /**
     * Number of segments contained in the complete file
     */
    public int getSegmentCount()
    {
        return getMessage().getInt(SEGMENT_COUNT, getOffset());
    }

    /**
     * List of repeater structures parsed from the Network Information File segment
     */
    public List<Repeater> getRepeaters()
    {
        if(mRepeaters == null)
        {
            mRepeaters = new ArrayList<>();

            int offset = getOffset() + REPEATER_STRUCTURE_START;

            for(int x = 0; x < 15; x++)
            {
                if(offset <= getMessage().length())
                {
                    Repeater repeater = new Repeater(getMessage(), offset, getNetwork());

                    if(repeater.isValid())
                    {
                        mRepeaters.add(repeater);
                    }

                    offset += REPEATER_STRUCTURE_LENGTH;
                }
            }
        }

        return mRepeaters;
    }

    public int getVersion()
    {
        return getMessage().getInt(VERSION, getOffset());
    }

    @Override
    public IHeader getHeader()
    {
        return null;
    }

    @Override
    public IPacket getPayload()
    {
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
