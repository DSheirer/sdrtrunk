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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola TDMA data channel active announcement.  Opcode 139.
 *
 * This messages correlates with Motorola FDMA TSBK OPCODE 22 which contained the same sequences x3060 and x803E and
 * was transmitted on the control channel for this TDMA data channel and seems to be frequency band and channel number
 * references.
 * 8B900FFF803EFF3060FF3060FF3060
 *
 * Example: Victoria Radio Network (VRN) - UHF control channel with a dedicated TDMA data channel.
 * It toggled through all four examples.
 * 1690C23F3060FFFF00001199
 * 9690C23F3060FFFF0000F5AD
 * 1690C23F803EFFFF000030AF
 * 9690C23F803EFFFF0000D49B
 */
public class MotorolaTDMADataChannel extends MacStructureVendor implements IFrequencyBandReceiver
{
    private static final IntField UNKNOWN_1 = IntField.length8(OCTET_4_BIT_24);
    private static final IntField FREQUENCY_BAND_1 = IntField.length4(OCTET_5_BIT_32);
    private static final IntField CHANNEL_NUMBER_1 = IntField.length12(OCTET_5_BIT_32 + 4);
    private static final IntField UNKNOWN_2 = IntField.length8(OCTET_7_BIT_48);
    private static final IntField FREQUENCY_BAND_2 = IntField.length4(OCTET_8_BIT_56);
    private static final IntField CHANNEL_NUMBER_2 = IntField.length12(OCTET_8_BIT_56 + 4);
    private static final IntField UNKNOWN_3 = IntField.length8(OCTET_10_BIT_72);
    private static final IntField FREQUENCY_BAND_3 = IntField.length4(OCTET_11_BIT_80);
    private static final IntField CHANNEL_NUMBER_3 = IntField.length12(OCTET_11_BIT_80 + 4);
    private static final IntField UNKNOWN_4 = IntField.length8(OCTET_13_BIT_96);
    private static final IntField FREQUENCY_BAND_4 = IntField.length4(OCTET_14_BIT_104);
    private static final IntField CHANNEL_NUMBER_4 = IntField.length12(OCTET_14_BIT_104 + 4);

    private APCO25Channel mChannel1;
    private APCO25Channel mChannel2;
    private APCO25Channel mChannel3;
    private APCO25Channel mChannel4;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaTDMADataChannel(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());

        if(hasChannel1())
        {
            sb.append(" ACTIVE CHAN1:").append(getChannel1());

            if(hasChannel2())
            {
                sb.append(" CHAN2:").append(getChannel2());

                if(hasChannel3())
                {
                    sb.append(" CHAN3:").append(getChannel3());

                    if(hasChannel1())
                    {
                        sb.append(" CHAN4:").append(getChannel4());
                    }
                }
            }
        }
        else
        {
            sb.append(" NOT ACTIVE");
        }

        sb.append(" MSG:").append(getSubMessage().toHexString());
        return sb.toString();
    }

    /**
     * Indicates if message has channel info.
     */
    private boolean hasChannel1()
    {
        return getInt(CHANNEL_NUMBER_1) != 0xF && getInt(FREQUENCY_BAND_1) != 0xFFF;
    }

    /**
     * Optional channel information.
     */
    public APCO25Channel getChannel1()
    {
        if(mChannel1 == null)
        {
            mChannel1 = APCO25Channel.create(getInt(FREQUENCY_BAND_1), getInt(CHANNEL_NUMBER_1));
        }

        return mChannel1;
    }

    /**
     * Indicates if message has channel info.
     */
    private boolean hasChannel2()
    {
        return getInt(CHANNEL_NUMBER_2) != 0xF && getInt(FREQUENCY_BAND_2) != 0xFFF &&
                getInt(FREQUENCY_BAND_2) != getInt(FREQUENCY_BAND_1) &&
                getInt(CHANNEL_NUMBER_2) != getInt(CHANNEL_NUMBER_1);
    }

    /**
     * Optional channel information.
     */
    public APCO25Channel getChannel2()
    {
        if(mChannel2 == null)
        {
            mChannel2 = APCO25Channel.create(getInt(FREQUENCY_BAND_2), getInt(CHANNEL_NUMBER_2));
        }

        return mChannel2;
    }

    /**
     * Indicates if message has channel info.
     */
    private boolean hasChannel3()
    {
        return getInt(CHANNEL_NUMBER_3) != 0xF && getInt(FREQUENCY_BAND_3) != 0xFFF &&
                getInt(FREQUENCY_BAND_3) != getInt(FREQUENCY_BAND_2) &&
                getInt(CHANNEL_NUMBER_3) != getInt(CHANNEL_NUMBER_2);
    }

    /**
     * Optional channel information.
     */
    public APCO25Channel getChannel3()
    {
        if(mChannel3 == null)
        {
            mChannel3 = APCO25Channel.create(getInt(FREQUENCY_BAND_3), getInt(CHANNEL_NUMBER_3));
        }

        return mChannel3;
    }

    /**
     * Indicates if message has channel info.
     */
    private boolean hasChannel4()
    {
        return getInt(CHANNEL_NUMBER_4) != 0xF && getInt(FREQUENCY_BAND_4) != 0xFFF &&
                getInt(FREQUENCY_BAND_4) != getInt(FREQUENCY_BAND_3) &&
                getInt(CHANNEL_NUMBER_4) != getInt(CHANNEL_NUMBER_3);
    }

    /**
     * Optional channel information.
     */
    public APCO25Channel getChannel4()
    {
        if(mChannel4 == null)
        {
            mChannel4 = APCO25Channel.create(getInt(FREQUENCY_BAND_4), getInt(CHANNEL_NUMBER_4));
        }

        return mChannel4;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of();
    }

    /**
     * List of active TDMA data channels.
     */
    public List<APCO25Channel> getActiveChannels()
    {
        List<APCO25Channel> channels = new ArrayList<>();
        if(hasChannel1())
        {
            channels.add(getChannel1());

            if(hasChannel2())
            {
                channels.add(getChannel2());

                if(hasChannel3())
                {
                    channels.add(getChannel3());

                    if(hasChannel4())
                    {
                        channels.add(getChannel4());
                    }
                }
            }

            return channels;
        }

        return List.of();
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        if(hasChannel1())
        {
            channels.add(getChannel1());

            if(hasChannel2())
            {
                channels.add(getChannel2());

                if(hasChannel3())
                {
                    channels.add(getChannel3());

                    if(hasChannel4())
                    {
                        channels.add(getChannel4());
                    }
                }
            }

            return channels;
        }

        return List.of();
    }
}
