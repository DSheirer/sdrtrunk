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

package io.github.dsheirer.module.decode.p25.phase1.message.ldu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import java.util.ArrayList;
import java.util.List;

/**
 * LDU voice frame
 */
public abstract class LDUMessage extends P25P1Message
{
    public static final int IMBE_FRAME_1 = 0;
    public static final int IMBE_FRAME_2 = 144;
    public static final int IMBE_FRAME_3 = 328;
    public static final int IMBE_FRAME_4 = 512;
    public static final int IMBE_FRAME_5 = 696;
    public static final int IMBE_FRAME_6 = 880;
    public static final int IMBE_FRAME_7 = 1064;
    public static final int IMBE_FRAME_8 = 1248;
    public static final int IMBE_FRAME_9 = 1424;
    public static final int[] LOW_SPEED_DATA = {1392, 1393, 1394, 1395, 1396, 1397, 1398, 1399, 1408, 1409, 1410, 1411,
            1412, 1413, 1414, 1415};
    private List<byte[]> mIMBIFrames;

    /**
     * Constructs an instance
     * @param message with data
     * @param nac code
     * @param timestamp for the message
     */
    public LDUMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    /**
     * Low speed data field contents.
     */
    public String getLowSpeedData()
    {
        return getMessage().getHex(LOW_SPEED_DATA, 4);
    }

    /**
     * Base message used by subclass implementations.
     * @return base message
     */
    public String getMessageStub()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(super.getMessageStub());
        sb.append(" VOICE LSD:");
        sb.append(getLowSpeedData());

        return sb.toString();
    }

    /**
     * Returns a 162 byte array containing 9 IMBE voice frames of 18-bytes* (144-bits) each.  Each frame is intact as
     * transmitted and requires deinterleaving, error correction, derandomizing, etc.
     */
    public synchronized List<byte[]> getIMBEFrames()
    {
        if(mIMBIFrames == null)
        {
            mIMBIFrames = new ArrayList<>();
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_1, IMBE_FRAME_1 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_2, IMBE_FRAME_2 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_3, IMBE_FRAME_3 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_4, IMBE_FRAME_4 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_5, IMBE_FRAME_5 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_6, IMBE_FRAME_6 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_7, IMBE_FRAME_7 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_8, IMBE_FRAME_8 + 144).toByteArray());
            mIMBIFrames.add(getMessage().get(IMBE_FRAME_9, IMBE_FRAME_9 + 144).toByteArray());
        }

        return mIMBIFrames;
    }
}
