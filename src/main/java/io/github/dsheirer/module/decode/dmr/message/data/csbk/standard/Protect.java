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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.csbk.CSBKMessage;
import io.github.dsheirer.module.decode.dmr.message.type.ProtectKind;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Protect
 */
public class Protect extends CSBKMessage
{
    private static final int[] PROTECT_KIND = new int[]{28, 29, 30};
    private static final int TALKGROUP_FLAG = 31;
    protected static final int[] DESTINATION = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55};
    protected static final int[] SOURCE = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
        73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mSourceRadio;
    private IntegerIdentifier mDestinationId;

    /**
     * Constructs a single-block CSBK instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     */
    public Protect(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType,
                   long timestamp, int timeslot)
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

        sb.append(" PROTECT: ").append(getProtectKind());
        sb.append(" FM:").append(getSourceRadio());
        sb.append(" TO:").append(getDestinationId());
        return sb.toString();
    }

    /**
     * Kind or type of protect command being issued.
     */
    public ProtectKind getProtectKind()
    {
        return ProtectKind.fromValue(getMessage().getInt(PROTECT_KIND));
    }

    /**
     * Source radio ID
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DmrTier3Radio.createFrom(getMessage().getInt(SOURCE));
        }

        return mSourceRadio;
    }

    /**
     * Destination ID
     */
    public IntegerIdentifier getDestinationId()
    {
        if(mDestinationId == null)
        {
            if(getMessage().get(TALKGROUP_FLAG))
            {
                mDestinationId = DMRTalkgroup.create(getMessage().getInt(DESTINATION));
            }
            else
            {
                mDestinationId = DmrTier3Radio.createTo(getMessage().getInt(DESTINATION));
            }
        }

        return mDestinationId;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceRadio());
            mIdentifiers.add(getDestinationId());
        }

        return mIdentifiers;
    }
}
