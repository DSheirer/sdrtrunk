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

package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25ExplicitChannel;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.VendorOSPMessage;
import java.util.Collections;
import java.util.List;

/**
 * Motorola TSBK Opcode 22 (0x16).  This seems to be a TDMA data channel announcement message.  A curious thing is the
 * frequency band for the channel number is an FDMA band.  I only have two examples of this, but in both examples
 * the frequency band and channel number align with known frequencies for the system and site and in one of those cases
 * it was an actual TDMA data channel, so this may or may not be correct.
 *
 * Example: PA-STARNET VHF control channel - not announcing a dedicated TDMA channel (15-4095).
 * 1690423FFFFFFFFF0000D458
 * 9690423FFFFFFFFF0000306C
 *
 * Example: Victoria Radio Network (VRN) Metro Mobile Radio - UHF control channel with a dedicated TDMA data channel.
 * https://www.radioreference.com/db/sid/7197 Site 1
 * 1690C23F3060FFFF00001199  0x3060 = 3-96 = 420.6125 MHz
 * 9690C23F3060FFFF0000F5AD
 * 1690C23F803EFFFF000030AF  0x803E = 8-62 = 467.9000 MHz.
 * 9690C23F803EFFFF0000D49B
 *
 * This messages correlates with Motorola TDMA MAC OPCODE 139 which contained both sequences x3060 and x803E that were
 * carried in separate Opcode 22 messages in the VRN control channel examples, but transmitted just this single message
 * repeatedly on the dedicated TDMA data channel.  TDMA Data Channel was active on 420.6125 MHz.
 * 8B900FFF803EFF3060FF3060FF3060
 *         ....  ....  ....  ....
 */
public class MotorolaExplicitTDMADataChannelAnnouncement extends VendorOSPMessage implements IFrequencyBandReceiver
{
    private static final IntField DOWNLINK_FREQUENCY_BAND = IntField.length4(OCTET_4_BIT_32);
    private static final IntField DOWNLINK_CHANNEL_NUMBER = IntField.length12(OCTET_4_BIT_32 + 4);
    private static final IntField UPLINK_FREQUENCY_BAND = IntField.length4(OCTET_6_BIT_48);
    private static final IntField UPLINK_CHANNEL_NUMBER = IntField.length12(OCTET_6_BIT_48 + 4);
    private APCO25Channel mChannel;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public MotorolaExplicitTDMADataChannelAnnouncement(P25P1DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        if(hasChannel())
        {
            sb.append(" ACTIVE CHAN:").append(getChannel());
            sb.append(" FREQ:").append(getChannel().getDownlinkFrequency());
        }
        else
        {
            sb.append(" NOT ACTIVE");
        }
        sb.append(" MSG:").append(getMessage().toHexString());
        return sb.toString();
    }

    /**
     * TDMA data channel.
     */
    public APCO25Channel getChannel()
    {
        if(mChannel == null && hasChannel())
        {
            if(hasUplink())
            {
                mChannel = APCO25ExplicitChannel.create(getInt(DOWNLINK_FREQUENCY_BAND), getInt(DOWNLINK_CHANNEL_NUMBER),
                        getInt(UPLINK_FREQUENCY_BAND), getInt(UPLINK_CHANNEL_NUMBER));
            }
            else
            {
                mChannel = APCO25Channel.create(getInt(DOWNLINK_FREQUENCY_BAND), getInt(DOWNLINK_CHANNEL_NUMBER));
            }
        }

        return mChannel;
    }

    /**
     * Indicates if the message has a data channel
     */
    public boolean hasChannel()
    {
        return getMessage().getInt(DOWNLINK_FREQUENCY_BAND) != 0xF && getMessage().getInt(DOWNLINK_CHANNEL_NUMBER) != 0xFFF;
    }

    /**
     * Indicates if the message has an uplink frequency band and channel number.
     */
    private boolean hasUplink()
    {
        return getMessage().getInt(UPLINK_FREQUENCY_BAND) != 0xF && getMessage().getInt(UPLINK_CHANNEL_NUMBER) != 0xFFF;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        if(hasChannel())
        {
            return List.of(getChannel());
        }

        return List.of();
    }
}
