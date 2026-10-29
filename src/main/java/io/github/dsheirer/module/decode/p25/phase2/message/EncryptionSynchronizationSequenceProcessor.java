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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomonErasureDecoder;
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
    /**
     * RS(44,16,29) shortened from RS(63,35,29).  The decoder has no mutable state.
     */
    private static final ReedSolomonErasureDecoder REED_SOLOMON = ReedSolomonErasureDecoder.p25_63_35_29();

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
        //code can correct v symbol errors and e erasures, where 2v + e <= 28, across the 16 info symbols provided by
        //ESS-B and the 28 parity symbols provided by ESS-A.  The symbols for any missing ESS-B fragments are first
        //decoded as zero valued symbols, which are correct for unencrypted calls (ie zero key ID and message
        //indicator), and then as erasures, which supports encrypted calls.
        if(mESSA != null && (mESSB1 != null || mESSB2 != null || mESSB3 != null || mESSB4 != null))
        {
            //We have to reverse the order of the information hexbits and the RS parity hexbits in the array for
            //the RS algorithm.
            int[] input = new int[63];
            int[] erasures = new int[12];
            int erasureCount = 0;

            for(int x = 27; x >= 0; x--)
            {
                input[27 - x] = mESSA.getInt(x * 6, x * 6 + 5);
            }

            BinaryMessage[] essB = {mESSB4, mESSB3, mESSB2, mESSB1};

            for(int fragment = 0; fragment < essB.length; fragment++)
            {
                int inputPointer = 28 + (fragment * 4);

                for(int x = 3; x >= 0; x--)
                {
                    if(essB[fragment] != null)
                    {
                        input[inputPointer] = essB[fragment].getInt(x * 6, x * 6 + 5);
                    }
                    else
                    {
                        erasures[erasureCount++] = inputPointer;
                    }

                    inputPointer++;
                }
            }

            int[] output = new int[63];
            boolean irrecoverableErrors = decode(input, output, erasures, 0);

            if(irrecoverableErrors && erasureCount > 0)
            {
                irrecoverableErrors = decode(input, output, erasures, erasureCount);
            }

            if(!irrecoverableErrors)
            {
                //Transfer error corrected output to a new binary message
                CorrectedBinaryMessage message = new CorrectedBinaryMessage(96);

                int pointer = 0;

                for(int x = 43; x >= 28; x--)
                {
                    message.load(pointer, 6, output[x]);
                    pointer += 6;
                }

                return new EncryptionSynchronizationSequence(message, mTimeslot, mTimestamp);
            }
        }

        return null;
    }

    /**
     * Decodes the RS(44,16,29) codeword.
     * @param input codeword
     * @param output for the corrected codeword
     * @param erasures positions of the erased symbols
     * @param erasureCount number of erasure positions to use
     * @return true if the codeword has irrecoverable errors or a correction is located in a shortened symbol.
     */
    private static boolean decode(int[] input, int[] output, int[] erasures, int erasureCount)
    {
        if(REED_SOLOMON.decode(input, output, erasures, erasureCount))
        {
            return true;
        }

        //The shortened symbols are known to be zero, so a correction in a shortened symbol is a decoding failure
        for(int x = 44; x < output.length; x++)
        {
            if(output[x] != 0)
            {
                return true;
            }
        }

        return false;
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
