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
 * Motorola Connect Plus - Fast GPS or Enhanced GPS / Data Revert Window Announcement.
 */
public class ConnectPlusDataRevertWindowAnnouncement extends CSBKMessage
{
    private static final int[] WINDOW = new int[]{16, 17, 18, 19, 20, 21, 22};
    private static final int[] SUPERFRAME = new int[]{24, 25, 26, 27};
    private static final int[] REPEATER = new int[]{28, 29, 30, 31}; //This may be the repeater number
    private static final int[] TARGET_RADIO = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55};

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mTargetRadio;

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
    public ConnectPlusDataRevertWindowAnnouncement(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CON+ ENHANCED DATA REVERT CHANNEL ANNOUNCE WINDOW:");
        sb.append(getSuperFrame()).append(".").append(getWindow());
        if(hasTargetRadio())
        {
            sb.append(" RESERVED FOR:").append(getTargetRadio());
        }
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * Radio identifier that this window is reserved for.
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createTo(getMessage().getInt(TARGET_RADIO));
        }

        return mTargetRadio;
    }

    /**
     * Indicates if this GPS transmit window is reserved for a specific radio identifier.
     */
    public boolean hasTargetRadio()
    {
        return getMessage().getInt(TARGET_RADIO) > 0;
    }

    /**
     * GPS Window Super Frame
     */
    public int getSuperFrame()
    {
        return getMessage().getInt(SUPERFRAME);
    }

    /**
     * GPS Window within a super frame
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
