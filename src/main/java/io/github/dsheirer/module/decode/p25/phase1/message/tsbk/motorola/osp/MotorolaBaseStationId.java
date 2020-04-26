/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.phase1.message.tsbk.motorola.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.tsbk.OSPMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MotorolaBaseStationId extends OSPMessage implements IFrequencyBandReceiver
{
    public static final int[] CHARACTER_1 = {16, 17, 18, 19, 20, 21};
    public static final int[] CHARACTER_2 = {22, 23, 24, 25, 26, 27};
    public static final int[] CHARACTER_3 = {28, 29, 30, 31, 32, 33};
    public static final int[] CHARACTER_4 = {34, 35, 36, 37, 38, 39};
    public static final int[] CHARACTER_5 = {40, 41, 42, 43, 44, 45};
    public static final int[] CHARACTER_6 = {46, 47, 48, 49, 50, 51};
    public static final int[] CHARACTER_7 = {52, 53, 54, 55, 56, 57};
    public static final int[] CHARACTER_8 = {58, 59, 60, 61, 62, 63};
    public static final int[] FREQUENCY_BAND = {64, 65, 66, 67};
    public static final int[] CHANNEL_NUMBER = {68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private String mCWID;
    private IChannelDescriptor mChannel;

    public MotorolaBaseStationId(P25P1DataUnitID dataUnitID, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitID, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        if(hasChannel())
        {
            sb.append(" CHAN:").append(getChannel());
            sb.append(" CWID:").append(getCWID());
        }
        else
        {
            sb.append(" CWID NOT SPECIFIED");
        }

        return sb.toString();
    }

    public boolean hasChannel()
    {
        return getMessage().getInt(CHANNEL_NUMBER) != 0;
    }


    public String getCWID()
    {
        if(mCWID == null)
        {
            StringBuilder sb = new StringBuilder();

            sb.append(getCharacter(CHARACTER_1));
            sb.append(getCharacter(CHARACTER_2));
            sb.append(getCharacter(CHARACTER_3));
            sb.append(getCharacter(CHARACTER_4));
            sb.append(getCharacter(CHARACTER_5));
            sb.append(getCharacter(CHARACTER_6));
            sb.append(getCharacter(CHARACTER_7));
            sb.append(getCharacter(CHARACTER_8));

            mCWID = sb.toString();
        }

        return mCWID;
    }

    private String getCharacter(int[] field)
    {
        int value = getMessage().getInt(field);

        if(value != 0)
        {
            return String.valueOf((char)(value + 43));
        }

        return "";
    }

    public IChannelDescriptor getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND),
                getMessage().getInt(CHANNEL_NUMBER));
        }

        return mChannel;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannel());
        return channels;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
