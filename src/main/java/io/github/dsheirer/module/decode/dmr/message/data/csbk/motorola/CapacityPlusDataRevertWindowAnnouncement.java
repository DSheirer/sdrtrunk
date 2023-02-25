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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import java.util.ArrayList;
import java.util.List;

/**
 * Capacity+ Window Announcement for Enhanced GPS/Data Revert Channel
 *
 * The window size dictates how many Windows happen within each data super frame.  You can infer the window size
 * by monitoring the maximum observed window value before rollover to zero as follows:
 *
 * Window Size: Number of Windows (hex) for 30-Second Data Super Frames
 * 5: 100 (0x52)
 * 6:  83 (0x53)
 * 7:  71 (0x47)
 * 8:  62 (0x3E)
 * 9:  55 (0x37)
 * 10:  50 (0x32)
 *
 * Window Size: Number of Windows (hex) for 2-Minute Data Super Frames
 * 1: 125 (0x7D)
 * 2:  62 (0x3E)
 *
 * Field UNKNOWN_2 may be reserved for a radio identifier that the target radio should transmit to.
 */
public class CapacityPlusDataRevertWindowAnnouncement extends CSBKMessage
{
    private static final int[] RESERVED = new int[]{16, 17, 18, 19, 20, 21, 22, 23,}; //Probably reserved for 32-bit addressing
    private static final int[] TARGET_RADIO = new int[]{24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] WINDOW = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SUPER_FRAME = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] UNKNOWN_1 = new int[]{56, 57, 58, 59, 60, 61, 62, 63}; //Always 0xFC
    private static final int[] UNKNOWN_2 = new int[]{64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79}; //Always 0x0000

    private RadioIdentifier mTargetRadio;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public CapacityPlusDataRevertWindowAnnouncement(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(!isValid())
        {
            sb.append("[CRC-ERROR] ");
        }

        sb.append("CC:").append(getSlotType().getColorCode());
        sb.append(" CSBK CAP+ ENHANCED DATA REVERT ANNOUNCEMENT");
        sb.append(" WINDOW:").append(getSuperFrame()).append(".").append(getWindow());

        if(hasTargetRadio())
        {
            sb.append(" RESERVED FOR RADIO:").append(getTargetRadio());
        }

        sb.append(" MSG:").append(getMessage().toHexString());

        return sb.toString();
    }

    /**
     * Indicates if there is a target radio specified in this message
     */
    public boolean hasTargetRadio()
    {
        return getMessage().getInt(TARGET_RADIO) > 0;
    }

    /**
     * Optional target radio identifier that the announced window is reserved for
     * @return radio identifier or NULL
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null && hasTargetRadio())
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_RADIO));
        }

        return mTargetRadio;
    }

    /**
     * Current Super Frame Number
     *
     * @return 0-15
     */
    public int getSuperFrame()
    {
        return getMessage().getInt(SUPER_FRAME);
    }

    /**
     * Current Window Number
     *
     * @return current window number, values range from 0 to 125 depending on repeater configuration
     */
    public int getWindow()
    {
        return getMessage().getInt(WINDOW);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(hasTargetRadio())
            {
                mIdentifiers.add(getTargetRadio());
            }
        }

        return mIdentifiers;
    }
}
