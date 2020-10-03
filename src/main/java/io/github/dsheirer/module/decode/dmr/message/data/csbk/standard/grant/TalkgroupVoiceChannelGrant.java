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

package io.github.dsheirer.module.decode.dmr.message.data.csbk.standard.grant;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.identifier.DMRTalkgroup;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.data.SlotType;
import io.github.dsheirer.module.decode.dmr.message.data.mbc.MBCContinuationBlock;

import java.util.ArrayList;
import java.util.List;

/**
 * Channel Grant - Voice - Talkgroup
 */
public class TalkgroupVoiceChannelGrant extends ChannelGrant
{
    private static final int EMERGENCY_FLAG = 30;

    private List<Identifier> mIdentifiers;
    private RadioIdentifier mSourceRadio;
    private TalkgroupIdentifier mDestinationTalkgroup;

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
    public TalkgroupVoiceChannelGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, SlotType slotType, long timestamp, int timeslot)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot);
    }

    /**
     * Constructs a multi-block MBC instance
     *
     * @param syncPattern for the CSBK
     * @param message bits
     * @param cach for the DMR burst
     * @param slotType for this message
     * @param timestamp
     * @param timeslot
     * @param multiBlock containing absolute frequency parameters
     */
    public TalkgroupVoiceChannelGrant(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                      SlotType slotType, long timestamp, int timeslot, MBCContinuationBlock multiBlock)
    {
        super(syncPattern, message, cach, slotType, timestamp, timeslot, multiBlock);
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

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        sb.append(" TALKGROUP VOICE CHANNEL GRANT FM:").append(getSourceRadio());
        sb.append(" TO:").append(getDestinationTalkgroup());
        sb.append(" CHAN:").append(getChannel());
        return sb.toString();
    }

    /**
     * Indicates if the emergency flag is set
     */
    public boolean isEmergency()
    {
        return getMessage().get(EMERGENCY_FLAG);
    }

    /**
     * Source radio identifier
     * @return
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DMRRadio.createFrom(getMessage().getInt(SOURCE));
        }

        return mSourceRadio;
    }

    /**
     * Destination radio identifier
     */
    public TalkgroupIdentifier getDestinationTalkgroup()
    {
        if(mDestinationTalkgroup == null)
        {
            mDestinationTalkgroup = DMRTalkgroup.create(getMessage().getInt(DESTINATION));
        }

        return mDestinationTalkgroup;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getChannel());
            mIdentifiers.add(getSourceRadio());
            mIdentifiers.add(getDestinationTalkgroup());
        }

        return mIdentifiers;
    }
}
