/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.edac.ReedSolomonErasureDecoder;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessageFactory;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureFailedRS;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract class for FACCH and SACCH signaling bearer messages
 */
public abstract class AbstractSignalingTimeslot extends Timeslot
{
    /**
     * RS(63,35,29) errors-and-erasures decoder shared by the signaling timeslots.  The decoder has no mutable state.
     */
    private static final ReedSolomonErasureDecoder REED_SOLOMON = ReedSolomonErasureDecoder.p25_63_35_29();
    /**
     * Index of the first message (information) symbol in the RS(63,35,29) codeword.  Lower indices are parity symbols.
     */
    private static final int FIRST_MESSAGE_INDEX = 28;
    /**
     * Soft decision decoding: number of additional (least reliable) received symbols erased for each decoding attempt
     * after the punctured parity symbols-only attempt fails.  Each step of 2 erasures reduces the error correction
     * capacity by one symbol error.
     */
    private static final int SOFT_ERASURE_STEP = 2;
    /**
     * Maximum total (punctured plus soft) erasures for soft decision decoding attempts.  Each attempt is a chance for a
     * miscorrection to pass the CRC check, and the miscorrection probability increases as the erasures consume the error
     * correction capacity.  Testing against Phase 2 recordings with injected noise (valid MAC messages at 9 dB in-band
     * C/N) and against random (noise) timeslots (SACCH CRC-valid messages per timeslot):
     *   punctured erasures only: 3125, 0/200k
     *   14: 3321, 0/200k
     *   20: 3391, 1/200k
     *   24: 8/200k
     *   28: 3405, 91/200k
     * FACCH (9 shortened symbols) produced no CRC-valid messages from random timeslots at any setting.
     */
    private static final int SOFT_ERASURE_MAX = 20;
    private static final float HALF_PI = (float)(Math.PI / 2.0);
    private static final float PI = (float)Math.PI;
    private static final float TWO_PI = (float)(2.0 * Math.PI);
    private float[] mSoftSymbols;

    /**
     * Constructs a scrambled signaling timeslot
     *
     * @param message with scrambled timeslot data
     * @param dataUnitID for the timeslot
     * @param scramblingSequence to unscramble the message
     * @param timeslot for the message
     * @param timestamp of the last transmitted bit
     */
    protected AbstractSignalingTimeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID,
                                        BinaryMessage scramblingSequence, int timeslot, long timestamp)
    {
        super(message, dataUnitID, scramblingSequence, timeslot, timestamp);
    }

    /**
     * Constructs an un-scrambled signaling timeslot
     *
     * @param message that is un-scrambled
     * @param dataUnitID for the timeslot
     * @param timeslot for the message
     * @param timestamp of the message
     */
    protected AbstractSignalingTimeslot(CorrectedBinaryMessage message, DataUnitID dataUnitID, int timeslot, long timestamp)
    {
        super(message, dataUnitID, timeslot, timestamp);
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }


    /**
     * Access Encoded MAC Information (EMI) message(s) carried by the signalling timeslot
     */
    public abstract List<MacMessage> getMacMessages();

    /**
     * Sets the soft symbols for this timeslot to enable soft decision decoding.
     * @param softSymbols (radians) for each of the 160 dibits in the timeslot.
     */
    public void setSoftSymbols(float[] softSymbols)
    {
        mSoftSymbols = softSymbols;
    }

    /**
     * Indicates if soft symbols are available for soft decision decoding.
     */
    protected boolean hasSoftSymbols()
    {
        return mSoftSymbols != null;
    }

    /**
     * Reliability of the dibit: distance (radians) of the soft symbol from the nearest symbol decision boundary
     * (0, +/- PI/2 and the +/- PI phase wrap).
     * @param dibitIndex in the timeslot
     * @return reliability
     */
    private float getDibitReliability(int dibitIndex)
    {
        float symbol = mSoftSymbols[dibitIndex];
        symbol -= TWO_PI * Math.round(symbol / TWO_PI);
        float magnitude = Math.abs(symbol);
        return Math.min(Math.min(magnitude, Math.abs(magnitude - HALF_PI)), PI - magnitude);
    }

    /**
     * Loads the hexbit (6-bit symbol) field value into the codeword and the reliability (weakest of the dibits that
     * carry the symbol) into the reliabilities array, when soft symbols are available.
     * @param codeword to load
     * @param reliabilities to load, or null
     * @param index of the symbol in the codeword
     * @param field for the symbol
     */
    protected void load(int[] codeword, float[] reliabilities, int index, IntField field)
    {
        codeword[index] = getInt(field);

        if(reliabilities != null)
        {
            float reliability = Float.MAX_VALUE;

            for(int bit = field.start(); bit <= field.end(); bit += 2)
            {
                reliability = Math.min(reliability, getDibitReliability(bit / 2));
            }

            reliabilities[index] = reliability;
        }
    }

    /**
     * Loads the hexbit (6-bit symbol) fragmented field value into the codeword and the reliability into the
     * reliabilities array, when soft symbols are available.
     * @param codeword to load
     * @param reliabilities to load, or null
     * @param index of the symbol in the codeword
     * @param field for the symbol
     */
    protected void load(int[] codeword, float[] reliabilities, int index, FragmentedIntField field)
    {
        codeword[index] = getInt(field);

        if(reliabilities != null)
        {
            float reliability = Float.MAX_VALUE;

            for(int bit : field.indices())
            {
                reliability = Math.min(reliability, getDibitReliability(bit / 2));
            }

            reliabilities[index] = reliability;
        }
    }

    /**
     * Creates a reliabilities array for the RS codeword when soft symbols are available.
     * @return reliabilities array or null.
     */
    protected float[] createReliabilities()
    {
        return hasSoftSymbols() ? new float[63] : null;
    }

    /**
     * Decodes the RS(63,35,29) codeword and creates the MAC message(s).  The punctured parity symbols are decoded as
     * erasures.  When soft symbol reliabilities are available and the decoded message fails the RS decode or the CRC
     * check, this applies generalized minimum distance decoding: repeats the decode with an increasing number of the
     * least reliable received symbols erased, until a decoded message passes the CRC check.
     *
     * @param codeword received symbols where index 0 to (puncturedCount - 1) are punctured parity symbols and the
     * indices above the final message index are shortened (zero) symbols.
     * @param reliabilities for each received symbol, or null.
     * @param puncturedCount number of punctured parity symbols
     * @param finalMessageIndex index of the final (first transmitted) message symbol
     * @param maxOctetIndex for the MAC message factory
     * @return MAC messages
     */
    protected List<MacMessage> decode(int[] codeword, float[] reliabilities, int puncturedCount, int finalMessageIndex,
                                      int maxOctetIndex)
    {
        int[] erasures = new int[REED_SOLOMON.getParityCount()];

        for(int x = 0; x < puncturedCount; x++)
        {
            erasures[x] = x;
        }

        int[] output = new int[63];
        boolean failed = decode(codeword, output, erasures, puncturedCount, finalMessageIndex);
        CorrectedBinaryMessage message = getMessage(codeword, output, puncturedCount, finalMessageIndex);

        if(!failed && MacMessageFactory.passesCRC(getDataUnitID(), message))
        {
            return MacMessageFactory.create(getTimeslot(), getDataUnitID(), message, getTimestamp(), maxOctetIndex);
        }

        if(reliabilities != null)
        {
            //Received symbol indices sorted by ascending reliability
            List<Integer> indices = new ArrayList<>();

            for(int x = puncturedCount; x <= finalMessageIndex; x++)
            {
                indices.add(x);
            }

            indices.sort((a, b) -> Float.compare(reliabilities[a], reliabilities[b]));
            int[] softOutput = new int[63];

            for(int erasureCount = puncturedCount + SOFT_ERASURE_STEP; erasureCount <= SOFT_ERASURE_MAX;
                erasureCount += SOFT_ERASURE_STEP)
            {
                for(int x = puncturedCount; x < erasureCount; x++)
                {
                    erasures[x] = indices.get(x - puncturedCount);
                }

                if(!decode(codeword, softOutput, erasures, erasureCount, finalMessageIndex))
                {
                    CorrectedBinaryMessage softMessage = getMessage(codeword, softOutput, puncturedCount,
                            finalMessageIndex);

                    if(MacMessageFactory.passesCRC(getDataUnitID(), softMessage))
                    {
                        return MacMessageFactory.create(getTimeslot(), getDataUnitID(), softMessage, getTimestamp(),
                                maxOctetIndex);
                    }
                }
            }
        }

        if(failed)
        {
            MacMessage macMessage = new MacMessage(getTimeslot(), getDataUnitID(), message, getTimestamp(),
                    new MacStructureFailedRS(message, 0));
            macMessage.setValid(false);
            List<MacMessage> macMessages = new ArrayList<>();
            macMessages.add(macMessage);
            return macMessages;
        }

        return MacMessageFactory.create(getTimeslot(), getDataUnitID(), message, getTimestamp(), maxOctetIndex);
    }

    /**
     * Decodes the codeword with the erasures.
     * @return true if the codeword has irrecoverable errors or a correction is located in a shortened symbol.
     */
    private static boolean decode(int[] codeword, int[] output, int[] erasures, int erasureCount, int finalMessageIndex)
    {
        if(REED_SOLOMON.decode(codeword, output, erasures, erasureCount))
        {
            return true;
        }

        //Shortened symbols are known to be zero, so a correction in a shortened symbol is a decoding failure
        for(int x = finalMessageIndex + 1; x < output.length; x++)
        {
            if(output[x] != 0)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Creates the MAC message from the decoded message symbols with the count of corrected bits in the received symbols.
     */
    private static CorrectedBinaryMessage getMessage(int[] codeword, int[] output, int puncturedCount,
                                                     int finalMessageIndex)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage((finalMessageIndex - FIRST_MESSAGE_INDEX + 1) * 6);
        int pointer = 0;

        for(int x = finalMessageIndex; x >= FIRST_MESSAGE_INDEX; x--)
        {
            message.load(pointer, 6, output[x]);
            pointer += 6;
        }

        for(int x = puncturedCount; x <= finalMessageIndex; x++)
        {
            if(output[x] != codeword[x])
            {
                message.incrementCorrectedBitCount(Integer.bitCount(output[x] ^ codeword[x]));
            }
        }

        return message;
    }
}
