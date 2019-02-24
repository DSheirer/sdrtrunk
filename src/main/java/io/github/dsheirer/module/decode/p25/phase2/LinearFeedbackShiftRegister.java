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

package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.BitSetFullException;

/**
 * APCO-25 Phase II Linear Feedback Shift Register for generating the scrambling sequence for descrambling
 * a Phase II inbound or outbound channel.
 *
 */
public class LinearFeedbackShiftRegister
{
    private static long MASK = 0xFFFFFFFFFFFl;
    private static long TAP_43 = (1l << 43);
    private static long TAP_33 = (1l << 33);
    private static long TAP_19 = (1l << 19);
    private static long TAP_14 = (1l << 14);
    private static long TAP_8 = (1l << 8);
    private static long TAP_3 = (1l << 3);

    private boolean mCurrentOutput;
    private long mRegisters;

    /**
     * Constructs an APCO25 Phase II external LFSR generator equivalent to
     * polynomial G(x)=x44 + x40 + x35 + x29 + x24 + x10 + x0 as defined in TIA-102.BBAC paragraph 7.2.5.
     */
    public LinearFeedbackShiftRegister()
    {
    }

    public void updateSeed(int wacn, int system, int nac)
    {
        mRegisters = ((0xFFFFF & wacn) << 20) + ((system & 0xFFF) << 12) + (nac & 0xFFF);

        if(mRegisters == 0)
        {
            mRegisters = 0xFFFFFFFFFFFl;
        }

        mCurrentOutput = (mRegisters & TAP_43) == TAP_43;
    }

    /**
     * Generates a 4320 bit (de)scrambling sequence for APCO25 Phase II channel superframe.
     *
     * @param wacn for the network from the Network Status Broadcast message.
     * @param system for the network from the Network Status Broadcast message.
     * @param nac or color code for the network from the Network Status Broadcast message.
     * @return scrambling sequence in a binary message
     */
    public BinaryMessage generateScramblingSequence(int wacn, int system, int nac)
    {
        updateSeed(wacn, system, nac);

        BinaryMessage sequence = new BinaryMessage(4320);

        try
        {
            for(int x = 0; x < 4320; x++)
            {
                sequence.add(next());
            }
        }
        catch(BitSetFullException e)
        {
            //This shouldn't happen
        }

        return sequence;
    }

    /**
     * Provides the next output bit from the LFSR
     */
    public boolean next()
    {
        boolean feedback = mCurrentOutput;
        feedback ^= (mRegisters & TAP_33) == TAP_33;
        feedback ^= (mRegisters & TAP_19) == TAP_19;
        feedback ^= (mRegisters & TAP_14) == TAP_14;
        feedback ^= (mRegisters & TAP_8) == TAP_8;
        feedback ^= (mRegisters & TAP_3) == TAP_3;
        mRegisters <<= 1;
        mRegisters &= MASK;

        if(feedback)
        {
            mRegisters |= 1;
        }

        mCurrentOutput = (mRegisters & TAP_43) == TAP_43;

        return mCurrentOutput;
    }

    public static void main(String[] args)
    {

    }
}
