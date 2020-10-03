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

package io.github.dsheirer.module.decode.dmr.message.data.lc.full.hytera;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.module.decode.dmr.identifier.DMRRadio;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.type.ServiceOptions;

/**
 * Hytera Full Link Control
 */
public abstract class HyteraFullLC extends FullLCMessage
{
    private static final int[] SERVICE_OPTIONS = new int[]{16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] FREE_REPEATER = new int[]{24, 25, 26, 27};
    private static final int[] PRIORITY_REPEATER = new int[]{28, 29, 30, 31};
    protected static final int[] TARGET_ADDRESS = new int[]{32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] PRIORITY_CALL_HASHED_ADDRESS = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = new int[]{56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] UNKNOWN_1 = new int[]{72, 73, 74, 75};
    private static final int[] UNKNOWN_2 = new int[]{76, 77, 78, 79};

    private ServiceOptions mServiceOptions;
    private RadioIdentifier mSourceRadio;

    /**
     * Constructs an instance.
     *
     * @param message for the link control payload
     */
    public HyteraFullLC(CorrectedBinaryMessage message, long timestamp, int timeslot)
    {
        super(message, timestamp, timeslot);
    }

    /**
     * Service options for the call
     */
    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Indicates if all free repreaters are busy
     */
    public boolean isAllChannelsBusy()
    {
        return getFreeRepeater() == 0;
    }


    /**
     * Free repeater number.
     *
     * @return repeater, or 0 for all repeaters busy
     */
    public int getFreeRepeater()
    {
        return getMessage().getInt(FREE_REPEATER);
    }

    /**
     * Indicates if there is a priority call on another repeater channel that users should monitor when their
     * talkgroup matches the priority call hashed address
     */
    public boolean hasPriorityCall()
    {
        return getPriorityCallRepeater() > 0;
    }

    /**
     * Indicates the repeater number that is forwarding a priority call
     */
    public int getPriorityCallRepeater()
    {
        return getMessage().getInt(PRIORITY_REPEATER);
    }

    /**
     * Hashed address of priority call
     */
    public String getPriorityCallHashedAddress()
    {
        return String.format("%02X", getMessage().getInt(PRIORITY_CALL_HASHED_ADDRESS)).toUpperCase();
    }

    /**
     * Unknown nibble one.  This value remains consistent for the FLC between the Voice Header and the Terminator
     * for calls.  It does not seem to have correlation to the channel/repeater number
     */
    public int getUnknownField1()
    {
        return getMessage().getInt(UNKNOWN_1);
    }

    /**
     * Unknown nibble two.  For each talkgroup, this field shows one value for the Voice Header and the same value but
     * bit-inverted in the Terminator.
     */
    public int getUnknownField2()
    {
        return getMessage().getInt(UNKNOWN_2);
    }

    /**
     * Source radio address
     */
    public RadioIdentifier getSourceRadio()
    {
        if(mSourceRadio == null)
        {
            mSourceRadio = DMRRadio.createFrom(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceRadio;
    }

}
