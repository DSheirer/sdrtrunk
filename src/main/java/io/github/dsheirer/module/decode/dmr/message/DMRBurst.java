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

package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
/**
 * Base DMR Burst Message class
 */
public abstract class DMRBurst extends DMRMessage
{
    public static final int PAYLOAD_1_START = 24;
    public static final int SYNC_START = 132;
    public static final int PAYLOAD_2_START = 180;

    private DMRSyncPattern mSyncPattern;
    private CACH mCACH;

    /**
     * DMR message frame.  This message is comprised of a 24-bit prefix and a 264-bit message frame.  Outbound base
     * station frames transmit a Common Announcement Channel (CACH) in the 24-bit prefix, whereas Mobile inbound frames
     * do not use the 24-bit prefix.
     *
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     * @param timestamp of the message
     */
    public DMRBurst(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
        mSyncPattern = syncPattern;
        mCACH = cach;
    }

    /**
     * Common Announcement Channel (CACH) message frame.  Note: check hasCACH() before accessing this method.
     * @return CACH frame or null if this message does not contain a CACH.
     */
    public CACH getCACH()
    {
        return mCACH;
    }

    /**
     * Indicates if this frame contains Common Announcement Channel (CACH) frame data.
     */
    public boolean hasCACH()
    {
        return getSyncPattern().hasCACH();
    }

    /**
     * DMR Sync pattern used by this message
     */
    public DMRSyncPattern getSyncPattern()
    {
        return mSyncPattern;
    }

    /**
     * Extracts the sync payload from between the two payload fragments.
     * @return binary message containing just the sync payload bits (64).
     */
    public BinaryMessage getSyncPayload()
    {
        return getMessage().get(SYNC_START, PAYLOAD_2_START);
    }
}