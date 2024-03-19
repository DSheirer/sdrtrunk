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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomon_44_16_29_P25;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Voice2Timeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Voice4Timeslot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase II Encryption Synchronization Sequence (ESS).  Provides encryption algorithm, key ID and message
 * indicator (key generator fill/seed sequence).
 *
 * The ESS is transmitted as a sequence of 5 fragments, each in a different voice timeslot.  A minimum of the ESS-A
 * and at least 1 of the 4 ESS-B fragments is required in order to attempt reed solomon error detection and correction.
 *
 * Once the minimum fragment requirement is met and the EDAC is successfully completed, further attempts to assign
 * ESS fragments are ignored.  Check isValid() after each fragment assignment to determine when to access the encryption
 * parameters.
 */
public class EncryptionSynchronizationSequenceProcessor
{
    private final static Logger mLog = LoggerFactory.getLogger(EncryptionSynchronizationSequenceProcessor.class);

    private BinaryMessage mESSA;
    private BinaryMessage mESSB1;
    private BinaryMessage mESSB2;
    private BinaryMessage mESSB3;
    private BinaryMessage mESSB4;
    private int mEssBCounter = 1;
    private int mTimeslot;
    private long mTimestamp;

    /**
     * Constructs an instance
     */
    public EncryptionSynchronizationSequenceProcessor(int timeslot)
    {
        mTimeslot = timeslot;
    }

    public void process(AbstractVoiceTimeslot abstractVoiceTimeslot)
    {
        if(abstractVoiceTimeslot instanceof Voice2Timeslot)
        {
            setESSA(((Voice2Timeslot)abstractVoiceTimeslot).getEssA());
            mTimestamp = abstractVoiceTimeslot.getTimestamp();
        }
        else if(abstractVoiceTimeslot instanceof Voice4Timeslot)
        {
            Voice4Timeslot voice4Timeslot = (Voice4Timeslot)abstractVoiceTimeslot;

            switch(mEssBCounter)
            {
                case 1:
                    setESSB1(voice4Timeslot.getEssB());
                    mEssBCounter++;
                    break;
                case 2:
                    setESSB2(voice4Timeslot.getEssB());
                    mEssBCounter++;
                    break;
                case 3:
                    setESSB3(voice4Timeslot.getEssB());
                    mEssBCounter++;
                    break;
                case 4:
                    setESSB4(voice4Timeslot.getEssB());
                    mEssBCounter = 1;
                    break;
            }
        }
    }

    /**
     * Resets this sequence so that a new sequence can be assembled
     */
    public void reset()
    {
        mESSA = null;
        mESSB1 = null;
        mESSB2 = null;
        mESSB3 = null;
        mESSB4 = null;
        mEssBCounter = 1;
    }


    /**
     * Constructs and returns an encryption synchronization sequence if possible, otherwise returns a null sequence.
     * The sequence can be created once the minimum quantity of fragments are loaded (ie ESS-A and one of the ESS-B fragments).
     */
    public EncryptionSynchronizationSequence getSequence()
    {
        //We need a minimum of ESS-A and at least one of the ESS-B sequences before we can decode.  The Reed Solomon
        //code can correct up to 14 symbol errors across the 16 info symbols provided by ESS-B and the 28 parity symbols
        //provided by ESS-A
        if(mESSA != null && (mESSB1 != null || mESSB2 != null || mESSB3 != null || mESSB4 != null))
        {
            //We have to reverse the order of the information hexbits and the RS parity hexbits in the array for
            //the RS algorithm.
            int[] input = new int[63];

            int inputPointer = 0;

            for(int x = 27; x >= 0; x--)
            {
                input[inputPointer++] = mESSA.getInt(x * 6, x * 6 + 5);
            }

            if(mESSB4 != null)
            {
                for(int x = 3; x >= 0; x--)
                {
                    input[inputPointer++] = mESSB4.getInt(x * 6, x * 6 + 5);
                }
            }
            else
            {
                inputPointer += 4;
            }

            if(mESSB3 != null)
            {
                for(int x = 3; x >= 0; x--)
                {
                    input[inputPointer++] = mESSB3.getInt(x * 6, x * 6 + 5);
                }
            }
            else
            {
                inputPointer += 4;
            }

            if(mESSB2 != null)
            {
                for(int x = 3; x >= 0; x--)
                {
                    input[inputPointer++] = mESSB2.getInt(x * 6, x * 6 + 5);
                }
            }
            else
            {
                inputPointer += 4;
            }

            if(mESSB1 != null)
            {
                for(int x = 3; x >= 0; x--)
                {
                    input[inputPointer++] = mESSB1.getInt(x * 6, x * 6 + 5);
                }
            }

            int[] output = new int[63];

            ReedSolomon_44_16_29_P25 rs = new ReedSolomon_44_16_29_P25();

            boolean irrecoverableErrors = rs.decode(input, output);

            if(!irrecoverableErrors)
            {
                //Transfer error corrected output to a new binary message
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);

                int pointer = 0;

                for(int x = 43; x >= 28; x--)
                {
                    if(output[x] != -1)
                    {
                        message.load(pointer, 6, output[x]);
                    }

                    pointer += 6;
                }

                return new EncryptionSynchronizationSequence(message, mTimeslot, mTimestamp);
            }
        }

        return null;
    }

    /**
     * ESS-B fragment 1
     */
    public void setESSB1(BinaryMessage essB1)
    {
        mESSB1 = essB1;
    }

    /**
     * ESS-B fragment 2
     */
    public void setESSB2(BinaryMessage essB2)
    {
        mESSB2 = essB2;
    }

    /**
     * ESS-B fragment 3
     */
    public void setESSB3(BinaryMessage essB3)
    {
        mESSB3 = essB3;
    }

    /**
     * ESS-B fragment 4
     */
    public void setESSB4(BinaryMessage essB4)
    {
        mESSB4 = essB4;
    }

    /**
     * ESS-A fragment
     */
    public void setESSA(BinaryMessage essA)
    {
        mESSA = essA;
    }
}
