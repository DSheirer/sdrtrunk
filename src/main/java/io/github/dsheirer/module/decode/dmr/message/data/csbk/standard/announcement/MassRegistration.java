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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.announcement;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DmrTier3Radio;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import java.util.ArrayList;
import java.util.List;

/**
 * DMR Tier III - Broadcast Mass Registration
 */
public class MassRegistration extends Announcement
{
    //Broadcast Parameters 1: 21-34
    private static final int[] RESERVED = new int[]{21, 22, 23, 24, 25};
    private static final int[] REGISTRATION_WINDOW = new int[]{26, 27, 28, 39};
    private static final int[] ALOHA_MASK = new int[]{30, 31, 32, 33, 34};

    //Broadcast Parameters 2: 56-79
    private static final int[] DESTINATION_RADIO = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70,
        71, 72, 73, 74, 75, 76, 77, 78, 79};

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mDestinationRadio;

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
    public MassRegistration(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
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
        sb.append(" MASS REGISTRATION");

        if(hasDestinationRadio())
        {
            sb.append(" TO:").append(getDestinationRadio());
        }

        sb.append(" ").append(getSystemIdentityCode().getModel());
        sb.append(" NETWORK:").append(getSystemIdentityCode().getNetwork());
        sb.append(" SITE:").append(getSystemIdentityCode().getSite());

        return sb.toString();
    }

    /**
     * Indicates if this message is addressing a specific radio
     */
    public boolean hasDestinationRadio()
    {
        return getMessage().getInt(DESTINATION_RADIO) != 0;
    }

    /**
     * Addressed destination radio identifier
     */
    public RadioIdentifier getDestinationRadio()
    {
        if(mDestinationRadio == null)
        {
            mDestinationRadio = DmrTier3Radio.createTo(getMessage().getInt(DESTINATION_RADIO));
        }

        return mDestinationRadio;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            if(hasDestinationRadio())
            {
                mIdentifiers.add(getDestinationRadio());
            }
            mIdentifiers.add(getSystemIdentityCode().getNetwork());
            mIdentifiers.add(getSystemIdentityCode().getSite());
        }

        return mIdentifiers;
    }
}
