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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.identifier.DMRNetwork;
import io.github.dsheirer.module.decode.dmr.identifier.DMRSite;

/**
 * Repeater Structure Parser
 */
public class Repeater
{
    private static int[] SITE = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static int[] REPEATER_ID = new int[]{8, 9, 10, 11};
    private static int CONTROL_CHANNEL_FLAG = 12;
    private static int[] UPLINK_FREQUENCY = new int[]{13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28,
        29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static int[] COLOR_CODE = new int[]{40, 41, 42, 43};
    private static int RESERVED = 44;
    private static int[] DOWNLINK_FREQUENCY = new int[]{45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60,
        61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    private CorrectedBinaryMessage mMessage;
    private int mOffset;
    private DMRNetwork mDMRNetwork;
    private DMRSite mSite;

    /**
     * Constructs an instance
     *
     * @param message containing a repeater structure
     * @param offset into the message to where the repeater structure starts
     */
    public Repeater(CorrectedBinaryMessage message, int offset, DMRNetwork dmrNetwork)
    {
        mMessage = message;
        mOffset = offset;
        mDMRNetwork = dmrNetwork;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("REPEATER:").append(getNetwork()).append(".").append(getSite()).append(".").append(getRepeaterNumber());
        sb.append(" CC:").append(getColorCode());
        sb.append(" DN:").append(getDownlinkFrequency() / 1E6d);
        sb.append(" UP:").append(getUplinkFrequency() / 1E6d);
        if(isControlChannel())
        {
            sb.append(" CONTROL");
        }
        return sb.toString();
    }

    public DMRNetwork getNetwork()
    {
        return mDMRNetwork;
    }

    /**
     * Message containing this repeater structure
     */
    private CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Offset into the message where this structure starts
     */
    private int getOffset()
    {
        return mOffset;
    }

    /**
     * Indicates if this repeater structure contains valid information
     * @return true if valid, or false if the downlink and uplink frequency values are the same.
     */
    public boolean isValid()
    {
        return getDownlinkFrequency() != getUplinkFrequency();
    }

    /**
     * Color code for the repeater
     */
    public int getColorCode()
    {
        return getMessage().getInt(COLOR_CODE, getOffset());
    }

    /**
     * Repeater receive / uplink frequency
     * @return frequency in Hertz
     */
    public long getUplinkFrequency()
    {
        return getMessage().getInt(UPLINK_FREQUENCY, getOffset()) * 10l;
    }

    /**
     * Repeater transmit / downlink frequency
     * @return frequency in Hertz
     */
    public long getDownlinkFrequency()
    {
        return getMessage().getInt(DOWNLINK_FREQUENCY, getOffset()) * 10l;
    }

    /**
     * Indicates if this is a control channel
     */
    public boolean isControlChannel()
    {
        return getMessage().get(CONTROL_CHANNEL_FLAG + getOffset());
    }

    /**
     * Repeater Number/ID
     *
     * @return id as a 1-based index
     */
    public int getRepeaterNumber()
    {
        return getMessage().getInt(REPEATER_ID, getOffset()) + 1;
    }

    public DMRSite getSite()
    {
        if(mSite == null)
        {
            mSite = DMRSite.create(getMessage().getInt(SITE, getOffset()));
        }

        return mSite;
    }
}
