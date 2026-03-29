/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Link Information Channel (LICH) enumeration identifies the channel configuration and direction and optional values.
 */
public enum LICH
{
    //Trunked - Control
    RCCH_INBOUND_SINGLE_CAC_SHORT(0x18, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_SHORT, Direction.INBOUND),
    RCCH_INBOUND_SINGLE_CAC_LONG(0x08, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_LONG, Direction.INBOUND),
    RCCH_INBOUND_SINGLE_UNKNOWN(-1, RFChannel.RCCH, Framing.UNKNOWN, Structure.UNKNOWN, Direction.INBOUND),
    RCCH_OUTBOUND_SINGLE_CAC_NORMAL(0x01, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_NORMAL, Direction.OUTBOUND),
    RCCH_OUTBOUND_SINGLE_CAC_IDLE(0x03, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_IDLE, Direction.OUTBOUND),
    RCCH_OUTBOUND_SINGLE_CAC_COMMON(0x05, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_COMMON, Direction.OUTBOUND),
    RCCH_OUTBOUND_SINGLE_UNKNOWN(-1, RFChannel.RCCH, Framing.SINGLE, Structure.CAC_NORMAL, Direction.OUTBOUND),

    //Trunked - Traffic
    RTCH_INBOUND_SUPER_VOICE_VOICE(0x36, RFChannel.RTCH, Framing.SUPER, Structure.VOICE_VOICE, Direction.INBOUND),
    RTCH_INBOUND_SUPER_VOICE_FACCH1(0x34, RFChannel.RTCH, Framing.SUPER, Structure.VOICE_FACCH1, Direction.INBOUND),
    RTCH_INBOUND_SUPER_FACCH1_VOICE(0x32, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_VOICE, Direction.INBOUND),
    RTCH_INBOUND_SUPER_FACCH1_FACCH1(0x30, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RTCH_INBOUND_SINGLE_FACCH1_FACCH1(0x20, RFChannel.RTCH, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RTCH_INBOUND_SUPER_IDLE(0x38, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RTCH_INBOUND_SINGLE_UDCH(0x2E, RFChannel.RTCH, Framing.SINGLE, Structure.UDCH, Direction.INBOUND),
    RTCH_INBOUND_SINGLE_FACCH2(0x28, RFChannel.RTCH, Framing.SINGLE, Structure.FACCH2, Direction.INBOUND),
    RTCH_INBOUND_UNKNOWN(-1, RFChannel.RTCH, Framing.UNKNOWN, Structure.UNKNOWN, Direction.INBOUND),

    RTCH_OUTBOUND_SUPER_VOICE_VOICE(0x37, RFChannel.RTCH, Framing.SUPER, Structure.VOICE_VOICE, Direction.OUTBOUND),
    RTCH_OUTBOUND_SUPER_VOICE_FACCH1(0x35, RFChannel.RTCH, Framing.SUPER, Structure.VOICE_FACCH1, Direction.OUTBOUND),
    RTCH_OUTBOUND_SUPER_FACCH1_VOICE(0x33, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_VOICE, Direction.OUTBOUND),
    RTCH_OUTBOUND_SUPER_FACCH1_FACCH1(0x31, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_OUTBOUND_SINGLE_FACCH1_FACCH1(0x21, RFChannel.RTCH, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_OUTBOUND_SUPER_IDLE(0x39, RFChannel.RTCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_OUTBOUND_SINGLE_UDCH(0x2F, RFChannel.RTCH, Framing.SINGLE, Structure.UDCH, Direction.OUTBOUND),
    RTCH_OUTBOUND_SINGLE_FACCH2(0x29, RFChannel.RTCH, Framing.SINGLE, Structure.FACCH2, Direction.OUTBOUND),
    RTCH_OUTBOUND_UNKNOWN(-1, RFChannel.RTCH, Framing.UNKNOWN, Structure.UNKNOWN, Direction.OUTBOUND),

    //Conventional repeater or direct SU-to-SU
    RDCH_INBOUND_SUPER_VOICE_VOICE(0x56, RFChannel.RDCH, Framing.SUPER, Structure.VOICE_VOICE, Direction.INBOUND),
    RDCH_INBOUND_SUPER_VOICE_FACCH1(0x54, RFChannel.RDCH, Framing.SUPER, Structure.VOICE_FACCH1, Direction.INBOUND),
    RDCH_INBOUND_SUPER_FACCH1_VOICE(0x52, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_VOICE, Direction.INBOUND),
    RDCH_INBOUND_SUPER_FACCH1_FACCH1(0x50, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RDCH_INBOUND_SINGLE_FACCH1_FACCH1(0x40, RFChannel.RDCH, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RDCH_INBOUND_SUPER_IDLE(0x58, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RDCH_INBOUND_SINGLE_UDCH(0x4E, RFChannel.RDCH, Framing.SINGLE, Structure.UDCH, Direction.INBOUND),
    RDCH_INBOUND_SINGLE_FACCH2(0x48, RFChannel.RDCH, Framing.SINGLE, Structure.FACCH2, Direction.INBOUND),
    RDCH_INBOUND_UNKNOWN(-1, RFChannel.RDCH, Framing.UNKNOWN, Structure.UNKNOWN, Direction.INBOUND),

    RDCH_OUTBOUND_SUPER_VOICE_VOICE(0x57, RFChannel.RDCH, Framing.SUPER, Structure.VOICE_VOICE, Direction.OUTBOUND),
    RDCH_OUTBOUND_SUPER_VOICE_FACCH1(0x55, RFChannel.RDCH, Framing.SUPER, Structure.VOICE_FACCH1, Direction.OUTBOUND),
    RDCH_OUTBOUND_SUPER_FACCH1_VOICE(0x53, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_VOICE, Direction.OUTBOUND),
    RDCH_OUTBOUND_SUPER_FACCH1_FACCH1(0x51, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RDCH_OUTBOUND_SINGLE_FACCH1_FACCH1(0x41, RFChannel.RDCH, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RDCH_OUTBOUND_SUPER_IDLE(0x59, RFChannel.RDCH, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RDCH_OUTBOUND_SINGLE_UDCH(0x4F, RFChannel.RDCH, Framing.SINGLE, Structure.UDCH, Direction.OUTBOUND),
    RDCH_OUTBOUND_SINGLE_FACCH2(0x49, RFChannel.RDCH, Framing.SINGLE, Structure.FACCH2, Direction.OUTBOUND),
    RDCH_OUTBOUND_UNKNOWN(-1, RFChannel.RDCH, Framing.UNKNOWN, Structure.UNKNOWN, Direction.OUTBOUND),

    //Trunked Composite Control
    RTCH_C_OUTBOUND_SUPER_VOICE_VOICE(0x77,  RFChannel.RTCHC, Framing.SUPER, Structure.VOICE_VOICE, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SUPER_VOICE_FACCH1(0x75, RFChannel.RTCHC, Framing.SUPER, Structure.VOICE_FACCH1, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SUPER_FACCH1_VOICE(0x73, RFChannel.RTCHC, Framing.SUPER, Structure.FACCH1_VOICE, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SUPER_FACCH1_FACCH1(0x71, RFChannel.RTCHC, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SINGLE_FACCH1_FACCH1(0x61, RFChannel.RTCHC, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SUPER_IDLE(0x79, RFChannel.RTCHC, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SINGLE_UDCH(0x6F, RFChannel.RTCHC, Framing.SINGLE, Structure.UDCH, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_SINGLE_FACCH2(0x69, RFChannel.RTCHC, Framing.SINGLE, Structure.FACCH2, Direction.OUTBOUND),
    RTCH_C_OUTBOUND_UNKNOWN(-1, RFChannel.RTCHC, Framing.UNKNOWN, Structure.UNKNOWN, Direction.OUTBOUND),

    //Type-D Trunking (RTCH2)
    RTCH_2_INBOUND_SUPER_VOICE_VOICE(0x76, RFChannel.RTCH2, Framing.SUPER, Structure.VOICE_VOICE, Direction.INBOUND),
    RTCH_2_INBOUND_SUPER_VOICE_FACCH1(0x74, RFChannel.RTCH2, Framing.SUPER, Structure.VOICE_FACCH1, Direction.INBOUND),
    RTCH_2_INBOUND_SUPER_FACCH1_VOICE(0x72, RFChannel.RTCH2, Framing.SUPER, Structure.FACCH1_VOICE, Direction.INBOUND),
    RTCH_2_INBOUND_SUPER_FACCH1_FACCH1(0x70, RFChannel.RTCH2, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RTCH_2_INBOUND_SINGLE_UDCH2(0x6E, RFChannel.RTCH2, Framing.SINGLE, Structure.UDCH2, Direction.INBOUND),
    RTCH_2_INBOUND_SINGLE_FACCH3(0x68, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH3, Direction.INBOUND),
    RTCH_2_INBOUND_SINGLE_FACCH1_GUARD(0x62, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH1_GUARD, Direction.INBOUND),
    RTCH_2_INBOUND_SINGLE_FACCH1_FACCH1(0x60, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.INBOUND),
    RTCH_2_INBOUND_UNKNOWN(-1, RFChannel.RTCH2, Framing.UNKNOWN, Structure.UNKNOWN, Direction.INBOUND),

    RTCH_2_OUTBOUND_SUPER_VOICE_VOICE(0x77,  RFChannel.RTCH2, Framing.SUPER, Structure.VOICE_VOICE, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SUPER_VOICE_FACCH1(0x75, RFChannel.RTCH2, Framing.SUPER, Structure.VOICE_FACCH1, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SUPER_FACCH1_VOICE(0x73, RFChannel.RTCH2, Framing.SUPER, Structure.FACCH1_VOICE, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SUPER_FACCH1_FACCH1(0x71, RFChannel.RTCH2, Framing.SUPER, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SINGLE_UDCH2(0x6F, RFChannel.RTCH2, Framing.SINGLE, Structure.UDCH2, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SINGLE_FACCH3(0x69, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH3, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SINGLE_FACCH1_GUARD(0x63, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH1_GUARD, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_SINGLE_FACCH1_FACCH1(0x61, RFChannel.RTCH2, Framing.SINGLE, Structure.FACCH1_FACCH1, Direction.OUTBOUND),
    RTCH_2_OUTBOUND_UNKNOWN(-1, RFChannel.RTCH2, Framing.UNKNOWN, Structure.UNKNOWN, Direction.OUTBOUND),

    UNKNOWN(-1, RFChannel.UNKNOWN, Framing.UNKNOWN, Structure.UNKNOWN, Direction.OUTBOUND);

    private int mValue;
    private RFChannel mRFChannel;
    private Framing mFraming;
    private Structure mStructure;
    private Direction mDirection;

    private static final Map<Integer, LICH> LOOKUP_MAP_STANDARD = new TreeMap<>();
    private static final Map<Integer, LICH> LOOKUP_MAP_TYPE_D = new TreeMap<>();

    static
    {
        for(LICH lich : LICH.values())
        {
            if(lich.getRFChannel() == RFChannel.RTCH2)
            {
                LOOKUP_MAP_TYPE_D.put(lich.getValue(), lich);
            }
            else
            {
                LOOKUP_MAP_STANDARD.put(lich.getValue(), lich);
            }
        }
    }

    /**
     * Constructs an instance
     * @param value as transmitted
     * @param rfChannel channel type
     * @param framing structure
     * @param structure options for the channel
     * @param direction inbound or outbound
     */
    LICH(int value, RFChannel rfChannel, Framing framing, Structure structure, Direction direction)
    {
        mValue = value;
        mRFChannel = rfChannel;
        mFraming = framing;
        mStructure = structure;
        mDirection = direction;
    }

    /**
     * Indicates if this frame contains a FACCH1 message in the first half of the frame
     */
    public boolean isFACCH1First()
    {
        return getStructure().isFACCH1First();
    }

    /**
     * Indicates if this frame contains a FACCH1 message in the second half of the frame
     */
    public boolean isFACCH1Second()
    {
        return getStructure().isFACCH1Second();
    }

    /**
     * Indicates if this frame contains a data fragment (FACCH2, FACCH3, UDCH or UDCH2)
     */
    public boolean isData()
    {
        return getStructure().isData();
    }


    /**
     * Indicates if the frame has audio frames in either or both of the payloads.
     */
    public boolean hasAudio()
    {
        return getStructure().isVoice();
    }

    /**
     * Indicates if this frame has voice data in the first half of the frame.
     */
    public boolean isVoiceFirst()
    {
        return getStructure() == Structure.VOICE_VOICE || getStructure() == Structure.VOICE_FACCH1;
    }

    /**
     * Indicates if this frame has voice data in the second half of the frame.
     */
    public boolean isVoiceSecond()
    {
        return getStructure() == Structure.VOICE_VOICE || getStructure() == Structure.FACCH1_VOICE;
    }

    /**
     * Indicates if the RDCH or RTCH frame has a SACCH field.
     */
    public boolean hasSACCH()
    {
        return getRFChannel() != RFChannel.RTCH2 && (getStructure().isFACCH1() || getStructure().isVoice());
    }

    /**
     * Indicates if this is a Type-D frame with a SCCH segment.
     */
    public boolean hasSCCH()
    {
        return getRFChannel() == RFChannel.RTCH2;
    }

    /**
     * Indicates if this is a (4x) super frame (true) or single frame (false)
     */
    public boolean isSuperFrame()
    {
        return getFraming() == Framing.SUPER;
    }

    /**
     * Look up the matching entry from the transmitted value.  The channel and direction parameters should be from
     * a tracker for the majority of observed values for the channel since the channel type and direction normally
     * won't change within the transmission.  These two additional values are for instance when the LICH has bit
     * errors that prevent a clean match up.
     * @param value transmitted
     * @param channel as tracked
     * @param direction as tracked
     * @return matching entry or UNKNOWN
     */
    public static LICH fromValue(int value, RFChannel channel, Direction direction)
    {
        if(channel == RFChannel.RTCH2)
        {
            if(LOOKUP_MAP_TYPE_D.containsKey(value))
            {
                return LOOKUP_MAP_TYPE_D.get(value);
            }
        }
        else
        {
            if(LOOKUP_MAP_STANDARD.containsKey(value))
            {
                return LOOKUP_MAP_STANDARD.get(value);
            }
        }

        switch(channel)
        {
            case RCCH:
                return direction == Direction.OUTBOUND ? RCCH_OUTBOUND_SINGLE_UNKNOWN : RCCH_INBOUND_SINGLE_UNKNOWN;
            case RTCH:
                return direction == Direction.OUTBOUND ? RTCH_OUTBOUND_UNKNOWN : RTCH_INBOUND_UNKNOWN;
            case RTCH2:
                return direction == Direction.OUTBOUND ? RTCH_2_OUTBOUND_UNKNOWN : RTCH_2_INBOUND_UNKNOWN;
            case RDCH:
                return direction == Direction.OUTBOUND ? RDCH_OUTBOUND_UNKNOWN : RDCH_INBOUND_UNKNOWN;
            case RTCHC:
                return RTCH_C_OUTBOUND_UNKNOWN;
            default:
                return UNKNOWN;
        }
    }

    /**
     * Transmitted value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * RF channel type
     */
    public RFChannel getRFChannel()
    {
        return mRFChannel;
    }

    /**
     * Framing
     */
    public Framing getFraming()
    {
        return mFraming;
    }

    /**
     * Options for the channel
     */
    public Structure getStructure()
    {
        return mStructure;
    }

    /**
     * Channel direction
     */
    public Direction getDirection()
    {
        return mDirection;
    }

    /**
     * Indicates if the channel direction is outbound (true) or inbound (false).
     * @return true if outbound
     */
    public boolean isOutbound()
    {
        return getDirection().equals(Direction.OUTBOUND);
    }

    /**
     * Indicates if the channel is carrying a long CAC (true) or short CAC (false)
     * @return true if long CAC
     */
    public boolean isLongCAC()
    {
        return this.equals(LICH.RCCH_INBOUND_SINGLE_CAC_LONG);
    }

    static void main()
    {
        List<LICH> liches = new ArrayList<>(Arrays.stream(LICH.values()).toList());
        liches.sort(Comparator.comparingInt(LICH::getValue));

        for(LICH setting : liches)
        {
            System.out.println(Integer.toHexString(setting.getValue()).toUpperCase() + ": " + setting);
        }
    }
}
