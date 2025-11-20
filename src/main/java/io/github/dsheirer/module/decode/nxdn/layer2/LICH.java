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

import java.util.Map;
import java.util.TreeMap;

/**
 * Link Information Channel (LICH) enumeration identifies the channel configuration and direction and optional values.
 */
public enum LICH
{
    //Trunked - Control
    RCCH_INBOUND_CONTROL_CAC_SHORT(0x18, RFChannel.RCCH, FunctionalChannel.CAC_SHORT, Option.DATA_COMMON, Direction.INBOUND),
    RCCH_INBOUND_CONTROL_CAC_LONG(0x08, RFChannel.RCCH, FunctionalChannel.CAC_LONG, Option.DATA_COMMON, Direction.INBOUND),
    RCCH_INBOUND_UNKNOWN(-1, RFChannel.RCCH, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.INBOUND),
    RCCH_OUTBOUND_CONTROL_CAC_NORMAL(0x01, RFChannel.RCCH, FunctionalChannel.CAC, Option.DATA_NORMAL, Direction.OUTBOUND),
    RCCH_OUTBOUND_CONTROL_CAC_IDLE(0x03, RFChannel.RCCH, FunctionalChannel.CAC, Option.DATA_IDLE, Direction.OUTBOUND),
    RCCH_OUTBOUND_CONTROL_CAC_COMMON(0x05, RFChannel.RCCH, FunctionalChannel.CAC, Option.DATA_COMMON, Direction.OUTBOUND),
    RCCH_OUTBOUND_UNKNOWN(-1, RFChannel.RCCH, FunctionalChannel.CAC, Option.UNKNOWN, Direction.OUTBOUND),

    //Trunked - Traffic
    RTCH_INBOUND_VOICE_SUPERFRAME_FACCH1_NONE(0x36, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.VOICE_ONLY, Direction.INBOUND),
    RTCH_INBOUND_VOICE_SUPERFRAME_FACCH1_SECOND(0x34, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_SECOND, Direction.INBOUND),
    RTCH_INBOUND_VOICE_SUPERFRAME_FACCH1_FIRST(0x32, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_FIRST, Direction.INBOUND),
    RTCH_INBOUND_VOICE_SUPERFRAME_FACCH1_BOTH(0x30, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RTCH_INBOUND_VOICE_NON_SUPERFRAME_FACCH1_BOTH(0x20, RFChannel.RTCH, FunctionalChannel.SACCH_NON_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RTCH_INBOUND_VOICE_SUPERFRAME_IDLE(0x38, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RTCH_INBOUND_DATA_UDCH(0x2E, RFChannel.RTCH, FunctionalChannel.UDCH, Option.UDCH, Direction.INBOUND),
    RTCH_INBOUND_DATA_FACCH2(0x28, RFChannel.RTCH, FunctionalChannel.UDCH, Option.FACCH2, Direction.INBOUND),
    RTCH_INBOUND_UNKNOWN(-1, RFChannel.RTCH, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.INBOUND),

    RTCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_NONE(0x37, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.VOICE_ONLY, Direction.OUTBOUND),
    RTCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_SECOND(0x35, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_SECOND, Direction.OUTBOUND),
    RTCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_FIRST(0x33, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_FIRST, Direction.OUTBOUND),
    RTCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_BOTH(0x31, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_OUTBOUND_VOICE_NON_SUPERFRAME_FACCH1_BOTH(0x21, RFChannel.RTCH, FunctionalChannel.SACCH_NON_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_OUTBOUND_VOICE_SUPERFRAME_IDLE(0x39, RFChannel.RTCH, FunctionalChannel.SACCH_SUPER_FRAME_IDLE, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_OUTBOUND_DATA_UDCH(0x2F, RFChannel.RTCH, FunctionalChannel.UDCH, Option.UDCH, Direction.OUTBOUND),
    RTCH_OUTBOUND_DATA_FACCH2(0x29, RFChannel.RTCH, FunctionalChannel.UDCH, Option.FACCH2, Direction.OUTBOUND),
    RTCH_OUTBOUND_UNKNOWN(-1, RFChannel.RTCH, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.OUTBOUND),

    //Trunked Composite Control
    RTCH_COMPOSITE_OUTBOUND_VOICE_SUPERFRAME_FACCH1_NONE(0x77, RFChannel.RTCH_C, FunctionalChannel.SACCH_SUPER_FRAME, Option.VOICE_ONLY, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_VOICE_SUPERFRAME_FACCH1_SECOND(0x75, RFChannel.RTCH_C, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_SECOND, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_VOICE_SUPERFRAME_FACCH1_FIRST(0x73, RFChannel.RTCH_C, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_FIRST, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_VOICE_SUPERFRAME_FACCH1_BOTH(0x71, RFChannel.RTCH_C, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_VOICE_NON_SUPERFRAME_FACCH1_BOTH(0x61, RFChannel.RTCH_C, FunctionalChannel.SACCH_NON_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_VOICE_SUPERFRAME_IDLE(0x79, RFChannel.RTCH_C, FunctionalChannel.SACCH_SUPER_FRAME_IDLE, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_DATA_UDCH(0x6F, RFChannel.RTCH_C, FunctionalChannel.UDCH, Option.UDCH, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_DATA_FACCH2(0x69, RFChannel.RTCH_C, FunctionalChannel.UDCH, Option.FACCH2, Direction.OUTBOUND),
    RTCH_COMPOSITE_OUTBOUND_UNKNOWN(-1, RFChannel.RTCH_C, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.OUTBOUND),

    //Conventional repeater or direct SU-to-SU
    RDCH_INBOUND_VOICE_SUPERFRAME_FACCH1_NONE(0x56, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.VOICE_ONLY, Direction.INBOUND),
    RDCH_INBOUND_VOICE_SUPERFRAME_FACCH1_SECOND(0x54, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_SECOND, Direction.INBOUND),
    RDCH_INBOUND_VOICE_SUPERFRAME_FACCH1_FIRST(0x52, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_FIRST, Direction.INBOUND),
    RDCH_INBOUND_VOICE_SUPERFRAME_FACCH1_BOTH(0x50, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RDCH_INBOUND_VOICE_NON_SUPERFRAME_FACCH1_BOTH(0x40, RFChannel.RDCH, FunctionalChannel.SACCH_NON_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RDCH_INBOUND_VOICE_SUPERFRAME_IDLE(0x58, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.INBOUND),
    RDCH_INBOUND_DATA_UDCH(0x4E, RFChannel.RDCH, FunctionalChannel.UDCH, Option.UDCH, Direction.INBOUND),
    RDCH_INBOUND_DATA_FACCH2(0x48, RFChannel.RDCH, FunctionalChannel.UDCH, Option.FACCH2, Direction.INBOUND),
    RDCH_INBOUND_UNKNOWN(-1, RFChannel.RDCH, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.INBOUND),

    RDCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_NONE(0x57, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.VOICE_ONLY, Direction.OUTBOUND),
    RDCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_SECOND(0x55, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_SECOND, Direction.OUTBOUND),
    RDCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_FIRST(0x53, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_FIRST, Direction.OUTBOUND),
    RDCH_OUTBOUND_VOICE_SUPERFRAME_FACCH1_BOTH(0x51, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RDCH_OUTBOUND_VOICE_NON_SUPERFRAME_FACCH1_BOTH(0x41, RFChannel.RDCH, FunctionalChannel.SACCH_NON_SUPER_FRAME, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RDCH_OUTBOUND_VOICE_SUPERFRAME_IDLE(0x59, RFChannel.RDCH, FunctionalChannel.SACCH_SUPER_FRAME_IDLE, Option.FACCH1_BOTH, Direction.OUTBOUND),
    RDCH_OUTBOUND_DATA_UDCH(0x4F, RFChannel.RDCH, FunctionalChannel.UDCH, Option.UDCH, Direction.OUTBOUND),
    RDCH_OUTBOUND_DATA_FACCH2(0x49, RFChannel.RDCH, FunctionalChannel.UDCH, Option.FACCH2, Direction.OUTBOUND),
    RDCH_OUTBOUND_UNKNOWN(-1, RFChannel.RDCH, FunctionalChannel.UNKNOWN, Option.UNKNOWN, Direction.OUTBOUND),

    UNKNOWN(-1, RFChannel.RCCH, FunctionalChannel.CAC, Option.UNKNOWN, Direction.OUTBOUND);

    private int mValue;
    private RFChannel mRFChannel;
    private FunctionalChannel mFunctionalChannel;
    private Option mOption;
    private Direction mDirection;

    private static final Map<Integer, LICH> LOOKUP_MAP = new TreeMap<>();

    static
    {
        for(LICH setting : LICH.values())
        {
            LOOKUP_MAP.put(setting.getValue(), setting);
        }
    }

    /**
     * Constructs an instance
     * @param value as transmitted
     * @param rfChannel channel type
     * @param functionalChannel channel type
     * @param option options for the channel
     * @param direction inbound or outbound
     */
    LICH(int value, RFChannel rfChannel, FunctionalChannel functionalChannel, Option option, Direction direction)
    {
        mValue = value;
        mRFChannel = rfChannel;
        mFunctionalChannel = functionalChannel;
        mOption = option;
        mDirection = direction;
    }

    /**
     * Indicates if this frame contains a FACCH1 message in the first half of the frame
     */
    public boolean isFACCH1First()
    {
        return getOption() == Option.FACCH1_FIRST || getOption() == Option.FACCH1_BOTH;
    }

    /**
     * Indicates if this frame contains a FACCH1 message in the second half of the frame
     */
    public boolean isFACCH1Second()
    {
        return getOption() == Option.FACCH1_SECOND || getOption() == Option.FACCH1_BOTH;
    }

    /**
     * Indicates if the frame has audio frames in either or both of the payloads.
     */
    public boolean hasAudio()
    {
        return getOption().hasAudio();
    }

    /**
     * Indicates if this frame has voice data in the first half of the frame.
     */
    public boolean isVoiceFirst()
    {
        return getOption() == Option.VOICE_ONLY || getOption() == Option.FACCH1_SECOND;
    }

    /**
     * Indicates if this frame has voice data in the second half of the frame.
     */
    public boolean isVoiceSecond()
    {
        return getOption() == Option.VOICE_ONLY || getOption() == Option.FACCH1_FIRST;
    }

    /**
     * Indicates if the RDCH or RTCH frame has a SACCH field.
     */
    public boolean hasSACCH()
    {
        return getOption() != Option.UDCH && getOption() != Option.FACCH2;
    }

    /**
     * Indicates if this is a SACCH super frame (true) or standalone (false).
     */
    public boolean isSACCHSuperFrame()
    {
        return getFunctionalChannel() == FunctionalChannel.SACCH_SUPER_FRAME;
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
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        switch(channel)
        {
            case RCCH:
                return direction == Direction.OUTBOUND ? RCCH_OUTBOUND_UNKNOWN : RCCH_INBOUND_UNKNOWN;
            case RTCH:
                return direction == Direction.OUTBOUND ? RTCH_OUTBOUND_UNKNOWN : RTCH_INBOUND_UNKNOWN;
            case RDCH:
                return direction == Direction.OUTBOUND ? RDCH_OUTBOUND_UNKNOWN : RDCH_INBOUND_UNKNOWN;
            case RTCH_C:
                return RTCH_COMPOSITE_OUTBOUND_UNKNOWN;
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
     * Functional channel type
     */
    public FunctionalChannel getFunctionalChannel()
    {
        return mFunctionalChannel;
    }

    /**
     * Options for the channel
     */
    public Option getOption()
    {
        return mOption;
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
        return getFunctionalChannel().equals(FunctionalChannel.CAC_LONG);
    }
}
