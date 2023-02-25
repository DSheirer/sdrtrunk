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
 * Motorola Connect Plus - Enhanced Data Revert - Window Grant
 */
public class ConnectPlusDataRevertWindowGrant extends CSBKMessage
{
    private static final int[] TARGET_ADDRESS = new int[]{16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31,
        32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SUPERFRAME = new int[]{48, 49, 50, 51};

    //TODO: this may not be correct
    private static final int[] WINDOW = new int[]{70, 71, 72, 73, 74};

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
    public ConnectPlusDataRevertWindowGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" CSBK CON+ ENHANCED DATA REVERT GRANT TO:").append(getTargetRadio());
        sb.append(" TRANSMIT IN WINDOW:").append(getSuperFrame()).append(".").append(getWindow());
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
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

    /**
     * Current radio user for the channel
     */
    public RadioIdentifier getTargetRadio()
    {
        if(mTargetRadio == null)
        {
            mTargetRadio = DMRRadio.createFrom(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetRadio;
    }


    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetRadio());
        }

        return mIdentifiers;
    }
}
