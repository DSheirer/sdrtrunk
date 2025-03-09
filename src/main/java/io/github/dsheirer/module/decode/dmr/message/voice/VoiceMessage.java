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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Base DMR Voice Message
 */
public abstract class VoiceMessage extends DMRBurst
{
    /**
     * Initialization vector (IV) fragments can be embedded into the AMBE voice frame for late entry to encrypted calls
     * using bits 71, 67, 63, and 59 from the interleaved (ie transmitted order) voice frame.
     */
    public static int[] FRAME_1_IV_FRAGMENT = new int[]{95, 91, 87, 83};
    public static int[] FRAME_2_IV_FRAGMENT = new int[]{215, 211, 207, 203};
    public static int[] FRAME_3_IV_FRAGMENT = new int[]{287, 283, 279, 275};

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param syncPattern
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                        int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
    }

    /**
     * AMBE audio CODEC encoded frames.
     */
    public List<byte[]> getAMBEFrames()
    {
        List<byte[]> frames = new ArrayList<>();
        byte[] frame_1 = new byte[9];
        byte[] frame_2 = new byte[9];
        byte[] frame_3 = new byte[9];
        for(int i = 0; i < 9; i++)
        {
            frame_1[i] = getMessage().getByte(24 + i * 8);
        }
        // copy first 4 byte
        for(int i = 0; i < 4; i++)
        {
            frame_2[i] = getMessage().getByte(96 + i * 8);
        }
        // 4 bits and 4 bits
        frame_2[4] = (byte)((getMessage().getByte(128) & 0xF0) | (getMessage().getByte(180) >> 4));
        // copy last 4 byte
        for(int i = 0; i < 4; i++)
        {
            frame_2[5 + i] = getMessage().getByte(184 + i * 8);
        }

        for(int i = 0; i < 9; i++)
        {
            frame_3[i] = getMessage().getByte(216 + i * 8);
        }
        frames.add(frame_1);
        frames.add(frame_2);
        frames.add(frame_3);
        return frames;
    }

    /**
     * Extracts the four low-order bits from the deinterleaved C3 vector of each of the three AMBE frames that carry
     * fragments of the initialization vector (IV) for encrypted calls.  Since we don't have access to the deinterleaved
     * voice frame here, we perform delinterleaving of the 4-bit nibble using the FRAME_x_IV_FRAGMENT constants.
     * <p>
     * See patent: https://patents.google.com/patent/EP2347540B1/en
     *
     * @return a three-byte array with the low-order four bits from each frame's C3 vector stored in the low nibble.
     */
    public byte[] getIvFragments()
    {
        byte[] fragments = new byte[3];
        fragments[0] = (byte)getMessage().getInt(FRAME_1_IV_FRAGMENT);
        fragments[1] = (byte)getMessage().getInt(FRAME_2_IV_FRAGMENT);
        fragments[2] = (byte)getMessage().getInt(FRAME_3_IV_FRAGMENT);
        return fragments;
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
}
