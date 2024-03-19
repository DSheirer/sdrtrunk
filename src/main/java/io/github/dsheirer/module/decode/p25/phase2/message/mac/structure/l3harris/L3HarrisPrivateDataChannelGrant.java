/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.l3harris;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.IP25ChannelGrantDetailProvider;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.DataServiceOptions;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * L3Harris Unknown Opcode 160 (0xA0), possible Private Data Channel Grant.
 *
 * Observed on L3Harris control channel transmitted to a radio after the radio was on an SNDCP data channel and the
 * controller was sending continuous TDULC with L3Harris Opcode 0x0A messages to the same radio.
 *
 * On returning to the phase 2 control channel, the following two messages were transmitted:
 *
 * LOCCH-U NAC:9/x009 SIGNAL CUSTOM/UNKNOWN VENDOR:HARRIS ID:A4 OPCODE:160 LENGTH:09 MSG:A0A409AC0312014871     (radio 0x014871 go to data channel 0x0312??)
 * LOCCH-U NAC:9/x009 SIGNAL CUSTOM/UNKNOWN VENDOR:HARRIS ID:A4 OPCODE:172 LENGTH:12 MSG:ACA40C000312014871980418 (from 0x014871 to 0x980418 unit-2-unit data channel grant?)
 *
 * Both messages seem to refer to a possible channel 0-786 (0x0312) so this may be a unit-2-unit private Phase 1 call
 * or maybe a private data call. Radio addresses: 0x014871 and 0x980418
 */
public class L3HarrisPrivateDataChannelGrant extends MacStructureVendor implements IFrequencyBandReceiver,
        IP25ChannelGrantDetailProvider
{
    private static final IntField FREQUENCY_BAND = IntField.length4(OCTET_5_BIT_32);
    private static final IntField CHANNEL_NUMBER = IntField.length12(OCTET_5_BIT_32 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_7_BIT_48);
    private APCO25Channel mChannel;
    private RadioIdentifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public L3HarrisPrivateDataChannelGrant(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("L3HARRIS MACO:160 PRIVATE CHANNEL GRANT TO:").append(getTargetAddress());
        sb.append(" CHAN:").append(getChannel()).append(" FREQ:").append(getChannel().getDownlinkFrequency());
        sb.append(" MSG:").append(getMessage().getSubMessage(getOffset(), getOffset() + (getLength() * 8)).toHexString());
        return sb.toString();
    }

    /**
     * Channel
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getInt(FREQUENCY_BAND), getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    /**
     * Target radio for this message.
     */
    public RadioIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        return Collections.singletonList(getChannel());
    }

    @Override
    public Identifier getSourceAddress()
    {
        return null;
    }

    @Override
    public ServiceOptions getServiceOptions()
    {
        return new DataServiceOptions(0);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
