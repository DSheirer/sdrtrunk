/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.mdc1200;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.mdc1200.identifier.MDC1200Identifier;
import io.github.dsheirer.protocol.Protocol;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class MDCMessage extends Message
{
    private static int[] SYNC1 = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22,
        23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};

    private static int[] OPCODE = {47, 46, 45, 44, 43, 42, 41, 40};
    private static int ANI_FLAG = 40;
    private static int DIRECTION_FLAG = 45;
    private static int ACKNOWLEDGE_REQUIRED_FLAG = 46;
    private static int PACKET_TYPE_FLAG = 47;
    private static int EMERGENCY_FLAG = 48;
    private static int[] ARGUMENT = {49, 50, 51, 52, 53, 54};
    private static int BOT_EOT_FLAG = 55;
    private static int[] DIGIT_2 = {59, 58, 57, 56};
    private static int[] DIGIT_1 = {63, 62, 61, 60};
    private static int[] DIGIT_4 = {67, 66, 65, 64};
    private static int[] DIGIT_3 = {71, 70, 69, 68};
    private static int[] IDENTITY = {63, 62, 61, 60, 59, 58, 57, 56, 71, 70, 69, 68, 67, 66, 65, 64};

    private CorrectedBinaryMessage mMessage;
    private MDC1200Identifier mFromIdentifier;
    private List<Identifier> mIdentifiers;

    public MDCMessage(CorrectedBinaryMessage message)
    {
        mMessage = message;
    }

    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    public MDC1200Identifier getFromIdentifier()
    {
        if(mFromIdentifier == null)
        {
            mFromIdentifier = MDC1200Identifier.createFrom(getMessage().getInt(IDENTITY));
        }

        return mFromIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getFromIdentifier());
        }

        return mIdentifiers;
    }

    public boolean isValid()
    {
        //TODO: add CRC and/or convolution decoding/repair
        return true;
    }

    public PacketType getPacketType()
    {
        if(mMessage.get(PACKET_TYPE_FLAG))
        {
            return PacketType.DATA;
        }
        else
        {
            return PacketType.CMND;
        }
    }

    public Acknowledge getResponse()
    {
        if(mMessage.get(ACKNOWLEDGE_REQUIRED_FLAG))
        {
            return Acknowledge.YES;
        }
        else
        {
            return Acknowledge.NO;
        }
    }

    public Direction getDirection()
    {
        if(mMessage.get(DIRECTION_FLAG))
        {
            return Direction.OUT;
        }
        else
        {
            return Direction.IN;
        }
    }

    public int getOpcode()
    {
        return mMessage.getInt(OPCODE);
    }

    public int getArgument()
    {
        return mMessage.getInt(ARGUMENT);
    }

    public boolean isEmergency()
    {
        return mMessage.get(EMERGENCY_FLAG);
    }


    public boolean isANI()
    {
        return mMessage.get(ANI_FLAG);
    }

    public boolean isBOT()
    {
        return mMessage.get(BOT_EOT_FLAG);
    }

    public boolean isEOT()
    {
        return !mMessage.get(BOT_EOT_FLAG);
    }

    public MDCMessageType getMessageType()
    {
        switch(getOpcode())
        {
            case 0:
                if(isEmergency())
                {
                    return MDCMessageType.EMERGENCY;
                }
            case 1:
                return MDCMessageType.ANI;
            default:
                return MDCMessageType.UNKNOWN;
        }
    }

    /**
     * Pads spaces onto the end of the value to make it 'places' long
     */
    public String pad(String value, int places, String padCharacter)
    {
        return StringUtils.rightPad(value, places, padCharacter);
    }

    /**
     * Pads an integer value with additional zeroes to make it decimalPlaces long
     */
    public String format(int number, int decimalPlaces)
    {
        return StringUtils.leftPad(Integer.valueOf(number).toString(), decimalPlaces, '0');
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.MDC1200;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("MDC1200 UNIT:").append(getFromIdentifier());
        if(isEmergency())
        {
            sb.append(" **EMERGENCY**");
        }

        if(isBOT())
        {
            sb.append(" BOT");
        }

        if(isEOT())
        {
            sb.append(" EOT");
        }

        sb.append(" OPCODE:").append(format(getOpcode(), 2));
        sb.append(" ARG:").append(format(getArgument(), 3));
        sb.append(" TYPE:").append(getPacketType().toString());
        sb.append(" ACK:").append(getResponse().toString());
        sb.append(" DIR:").append(pad(getDirection().toString(), 3, " "));

        return sb.toString();
    }

    private enum PacketType
    {
        CMND, DATA
    }

    private enum Acknowledge
    {
        YES, NO
    }

    private enum Direction
    {
        IN, OUT
    }
}
