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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.ahoy;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.type.Tier3Gateway;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III Ahoy - Stun, Revive or Kill a Mobile Radio
 */
public class StunReviveKill extends Ahoy
{
    private RadioIdentifier mSourceRadio;
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
    public StunReviveKill(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        sb.append(" ").append(getCommand()).append(" RADIO:").append(getTargetAddress());
        sb.append(" FM:").append(getSourceRadio());

        return sb.toString();
    }

    /**
     * Gateway address from the multi-purpose field
     */
    public Tier3Gateway getGateway()
    {
        return Tier3Gateway.fromValue(getMultiPurposeFieldValue());
    }

    /**
     * Command for this message
     */
    public String getCommand()
    {
        switch(getGateway())
        {
            case KILLI:
                return "KILL";
            case STUNI:
                if(isStunCommand())
                {
                    return "STUN";
                }
                else
                {
                    return "REVIVE";
                }
            default:
                return "UNKNOWN";
        }
    }

    /**
     * Indicates if this is a Stun command
     */
    public boolean isStunCommand()
    {
        return !getMessage().get(SERVICE_KIND_FLAG);
    }

    /**
     * Indicates if this is a Revive command
     */
    public boolean isReviveCommand()
    {
        return !getMessage().get(SERVICE_KIND_FLAG);
    }

    /**
     * Source radio identifier
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DMRRadio.createFrom(getMultiPurposeFieldValue());
        }

        return mSourceRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceRadio());
        }

        return mIdentifiers;
    }
}
