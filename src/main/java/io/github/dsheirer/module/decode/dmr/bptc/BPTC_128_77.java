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

package io.github.dsheirer.module.decode.dmr.bptc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming16;
import io.github.dsheirer.edac.soft.SoftBlockCode;
import io.github.dsheirer.edac.soft.SoftProductCodeDecoder;

/**
 * Block product turbo code (BPTC) for decoding Full Link Control messages reassembled from the 32-bit payload fragments
 * in each of the Voice B-E messages.
 */
public class BPTC_128_77 extends BPTCBase
{
    private static final int LENGTH = 128;
    private static final int ROW_COUNT = 8;
    private static final int COLUMN_COUNT = 16;
    //Maximum correction cost (sum of the normalized channel soft bit magnitudes for the corrected bits) for a soft
    //decision result.  The embedded link control is only protected by a 5-bit checksum, so soft decisions that
    //require correcting high reliability bits are rejected to avoid false decodes.  Noise injection testing: all
    //soft decisions with a cost up to 3.0 were correct, whereas most of those with a higher cost were false decodes.
    private static final float SOFT_MAX_CORRECTION_COST = 3.0f;
    //Maximum corrected bit count for a hard decision result.  Hard decisions that correct more bits are treated as
    //uncorrectable to avoid false decodes, given the weak 5-bit checksum.  Noise injection testing: all hard
    //decisions with more than 6 corrected bits were false decodes.
    private static final int HARD_MAX_CORRECTED_BITS = 6;
    //Soft decision decoder for the Hamming(16,11,4) rows and even parity columns
    private static final SoftProductCodeDecoder SOFT_DECODER = new SoftProductCodeDecoder(
            SoftBlockCode.fromCodewordTest(COLUMN_COUNT, word -> Hamming16.getSyndrome(toMessage(word), 0) == 0),
            ROW_COUNT, SOFT_MAX_CORRECTION_COST);

    /**
     * Creates a binary message from the word where bit i of the word is message bit i.
     */
    private static BinaryMessage toMessage(int word)
    {
        BinaryMessage message = new BinaryMessage(COLUMN_COUNT);

        for(int i = 0; i < COLUMN_COUNT; i++)
        {
            if((word & (1 << i)) != 0)
            {
                message.set(i);
            }
        }

        return message;
    }

    public BPTC_128_77()
    {
        super(new Hamming16(), 16, 8);
    }

    /**
     * Allow the base class to attempt to correct multiple rows of 2-bit errors each
     */
    @Override
    protected boolean canCorrectMultiRow2BitErrors()
    {
        return true;
    }

    /**
     * Performs error detection and correction and extracts the payload from the BPTC encoded message.
     * @param message with BPTC encoding.
     * @return decoded message.
     */
    public CorrectedBinaryMessage extract(BinaryMessage message)
    {
        return extract(message, null);
    }

    /**
     * Performs error detection and correction and extracts the payload from the BPTC encoded message.  Hard decision
     * results that exceed the maximum corrected bit count are treated as uncorrectable.  When the hard decision
     * decoder can't correct the message and soft bits are provided, applies iterative soft decision decoding.
     * @param message with BPTC encoding.
     * @param softBits for the 128 (interleaved) bits of the message where a positive value favors a 1 bit, or null.
     * @return decoded message.
     */
    public CorrectedBinaryMessage extract(BinaryMessage message, float[] softBits)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message);
        correct(deinterleaved);

        if(deinterleaved.getCorrectedBitCount() > HARD_MAX_CORRECTED_BITS)
        {
            deinterleaved.setCorrectedBitCount(ERRORS_NOT_CORRECTED);
        }

        if(softBits != null && softBits.length == LENGTH && deinterleaved.getCorrectedBitCount() < 0)
        {
            CorrectedBinaryMessage softDecoded = decodeSoft(deinterleave(softBits));

            if(softDecoded != null)
            {
                deinterleaved = softDecoded;
            }
        }

        CorrectedBinaryMessage extractedMessage = new CorrectedBinaryMessage(77);
        int pointer = 0;

        //Extract the message.  Row 1 and 2 are 11 bits.  Rows 3-7 are 10 bits.
        for(int row = 0; row < 2; row++)
        {
            for(int column = 0; column < 11; column++)
            {
                extractedMessage.set(pointer++, deinterleaved.get(row * 16 + column));
            }
        }

        for(int row = 2; row < 7; row++)
        {
            for(int column = 0; column < 10; column++)
            {
                extractedMessage.set(pointer++, deinterleaved.get(row * 16 + column));
            }

            //Transfer the checksum parity bit from this row
            extractedMessage.set(70 + row, deinterleaved.get(row * 16 + 10));
        }

        extractedMessage.setCorrectedBitCount(deinterleaved.getCorrectedBitCount());
        return extractedMessage;
    }

    /**
     * Deinterleaves the transmitted soft bits.
     * @param interleaved soft bits
     * @return deinterleaved soft bits.
     */
    public static float[] deinterleave(float[] interleaved)
    {
        float[] deinterleaved = new float[LENGTH];
        deinterleaved[127] = interleaved[127];

        for(int i = 0; i < 127; i++)
        {
            deinterleaved[i] = interleaved[(i * 8) % 127];
        }

        return deinterleaved;
    }

    /**
     * Iterative soft decision decoding of the de-interleaved BPTC(128,77) soft bits.  Alternately decodes the
     * Hamming(16,11,4) rows using Chase-Pyndiah decoding and the even parity columns using min-sum decoding,
     * exchanging extrinsic soft information, until the decisions form a valid product codeword.  Rejects decisions
     * where the correction cost exceeds the maximum.
     * @param channel de-interleaved soft bits where a positive value favors a 1 bit.
     * @return decoded message with the corrected bit count set, or null if decoding doesn't converge or the correction
     * cost exceeds the maximum.
     */
    public CorrectedBinaryMessage decodeSoft(float[] channel)
    {
        return SOFT_DECODER.decode(channel);
    }

    /**
     * Deinterleaves the transmitted message.
     * @param interleaved message
     * @return deinterleaved message.
     */
    public CorrectedBinaryMessage deinterleave(BinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(128);
        int index;
        deinterleaved.set(127, interleaved.get(127));
        for(int i = 0; i < 127; i++)
        {
            index = (i * 8) % 127;
            deinterleaved.set(i, interleaved.get(index));
        }

        return deinterleaved;
    }

    public static void main(String[] args)
    {
        BPTC_128_77 bptc = new BPTC_128_77();
        String deinterleavedRaw = "00000100000101010000001000111000100000000001001101000010001001000000000010101000111100000000001000011000100010110010100100100000";
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(BinaryMessage.load(deinterleavedRaw));

        String deinterleavedReference = "00000100000101010000011000111001100000000001001101000010001101100000000110101000111100000000101000011000100010110010100100100000"; //No errors
        CorrectedBinaryMessage deinterleavedReferenceMessage = new CorrectedBinaryMessage(BinaryMessage.load(deinterleavedReference));

//        String interleaved = "00000000000000000000000000010010000100010000001000000010000100010000001100000110001101100000001100000101001000010010010100110011"; //Under test
//        CorrectedBinaryMessage interleavedMessage = new CorrectedBinaryMessage(BinaryMessage.load(interleaved));
//        CorrectedBinaryMessage deinterleaved = deinterleave(interleavedMessage);


        String deinterleavedUncorrected = deinterleaved.toString();
        bptc.logDiagnostic(deinterleaved);
        bptc.correct(deinterleaved);

        System.out.println("DEINTERLEAVED: " + deinterleavedUncorrected);
        System.out.println("    CORRECTED: " + deinterleaved);
        System.out.println("    REFERENCE: " + deinterleavedReferenceMessage);

        deinterleaved.xor(deinterleavedReferenceMessage);
        System.out.println("        DELTA: " + deinterleaved);

        if(deinterleaved.cardinality() > 0)
        {
            System.out.println("--------------------------------");
            System.out.println("Residual Error Map:");
            bptc.logErrorMap(deinterleaved);
            System.out.println("Columns:" + bptc.getColumnErrors(deinterleaved) + " Rows:" + bptc.getRowErrors(deinterleaved));
            System.out.println("--------------------------------");
        }
        else
        {
            System.out.println("--------------------------------");
            System.out.println("Message Was Fully Corrected");
            System.out.println("--------------------------------");
        }
    }
}
