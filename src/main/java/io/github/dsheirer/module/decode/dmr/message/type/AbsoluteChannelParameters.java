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

package io.github.dsheirer.module.decode.dmr.message.type;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.channel.DMRAbsoluteChannel;

/**
 * Absolute channel frequency structure.  This structure is used in Multi-Block CSBK to define channel parameters.
 *
 * See: TS 102 361-4 Paragraph 7.1.1.1.2 CG_AP
 */
public class AbsoluteChannelParameters extends AbstractStructure
{
    private static final int LAST_BLOCK_FLAG = 0;
    private static final int ENCRYPTION_FLAG = 1;
    private static final int[] OPCODE = new int[]{2, 3, 4, 5, 6, 7};
    private static final int[] COLOR_CODE = new int[]{12, 13, 14, 15};

    //Note: the ICD only defines a value of 0 for this field, so it's currently unused.
    private static final int[] CDEF_TYPE = new int[]{16, 17, 18, 19};
    private static final int[] LCN = new int[]{22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33};
    private static final int[] UPLINK_FREQUENCY_MHZ = new int[]{34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] UPLINK_FREQUENCY_125_HZ = new int[]{44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56};
    private static final int[] DOWNLINK_FREQUENCY_MHZ = new int[]{57, 58, 59, 60, 61, 62, 63, 64, 65, 66};
    private static final int[] DOWNLINK_FREQUENCY_125_HZ = new int[]{67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private DMRAbsoluteChannel mChannel;
    private int mTimeslot;

    /**
     * Constructs an instance
     *
     * @param message containing this structure
     * @param offset into the message
     * @param timeslot for the channel
     */
    public AbsoluteChannelParameters(CorrectedBinaryMessage message, int offset, int timeslot)
    {
        super(message, offset);
        mTimeslot = timeslot;
    }

    /**
     * DMR Channel defined by this structure
     * @return channel
     */
    public DMRAbsoluteChannel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = new DMRAbsoluteChannel(getChannelNumber(), mTimeslot, getDownlinkFrequency(), getUplinkFrequency());
        }

        return mChannel;
    }

    /**
     * Color code for the channel.  Note: default value is 0 if unspecified.
     */
    public int getColorCode()
    {
        return getMessage().getInt(COLOR_CODE, getOffset());
    }

    /**
     * Indicates if this is the Last/Final block in a Multi-Block sequence
     */
    public boolean isLastBlock()
    {
        return getMessage().get(LAST_BLOCK_FLAG + getOffset());
    }

    /**
     * Indicates if this block is encrypted
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_FLAG + getOffset());
    }

    /**
     * Opcode - repeated from the first block in the multi-block sequence
     */
    public int getOpcodeValue()
    {
        return getMessage().getInt(OPCODE);
    }

    /**
     * Logical Channel Number
     */
    public int getChannelNumber()
    {
        return getMessage().getInt(LCN, getOffset());
    }

    /**
     * Channel uplink (MS -> Tower) frequency
     * @return frequency in Hz
     */
    public long getUplinkFrequency()
    {
        long frequency = (long)(getMessage().getInt(UPLINK_FREQUENCY_MHZ, getOffset()) * 1E6);
        frequency += getMessage().getInt(UPLINK_FREQUENCY_125_HZ) * 125;
        return frequency;
    }

    /**
     * Channel downlink (Tower -> MS) frequency
     * @return frequency in Hz
     */
    public long getDownlinkFrequency()
    {
        long frequency = (long)(getMessage().getInt(DOWNLINK_FREQUENCY_MHZ, getOffset()) * 1E6);
        frequency += getMessage().getInt(DOWNLINK_FREQUENCY_125_HZ) * 125;
        return frequency;
    }
}
