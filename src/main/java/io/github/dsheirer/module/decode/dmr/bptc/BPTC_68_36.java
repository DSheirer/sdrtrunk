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
import io.github.dsheirer.edac.Hamming17;
import io.github.dsheirer.edac.soft.SoftBlockCode;
import io.github.dsheirer.edac.soft.SoftProductCodeDecoder;

/**
 * Block product turbo code (BPTC) for decoding Short Link Control reassembled from CACH fragments.
 */
public class BPTC_68_36 extends BPTCBase
{
    private static final int LENGTH = 68;
    private static final int ROW_COUNT = 4;
    private static final int COLUMN_COUNT = 17;
    //Maximum correction cost (sum of the normalized channel soft bit magnitudes for the corrected bits) for a soft
    //decision result.  No limit is applied since the CRC-8 check rejects most false decodes.  Noise injection testing:
    //754 correct and 2 false soft decisions, where the false decisions had costs within the range of the correct
    //decisions, so a cost limit would reject far more correct decisions than false decisions.
    private static final float SOFT_MAX_CORRECTION_COST = Float.MAX_VALUE;
    //Soft decision decoder for the Hamming(17,12) rows and even parity columns
    private static final SoftProductCodeDecoder SOFT_DECODER = new SoftProductCodeDecoder(
            SoftBlockCode.fromCodewordTest(COLUMN_COUNT, word -> Hamming17.getSyndrome(toMessage(word), 0) == 0),
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

    public BPTC_68_36()
    {
        super(new Hamming17(), 17, 4);
    }

    /**
     * Performs error detection and correction and extracts the payload from the BPTC encoded message.
     * @param message with BPTC encoding.
     * @return decoded message.
     */
    public CorrectedBinaryMessage extract(BinaryMessage message)
    {
        CorrectedBinaryMessage deinterleaved = deinterleave(message);
        correct(deinterleaved);
        return extractPayload(deinterleaved);
    }

    /**
     * Applies soft decision decoding to the soft bits and extracts the 36-bit payload.  Can be used when the hard
     * decision result fails BPTC error correction or the CRC-8 check.
     * @param softBits for the 68 (interleaved) bits of the message where a positive value favors a 1 bit.
     * @return decoded message, or null if soft decision decoding doesn't converge.
     */
    public CorrectedBinaryMessage extractSoft(float[] softBits)
    {
        if(softBits == null || softBits.length != LENGTH)
        {
            return null;
        }

        CorrectedBinaryMessage decoded = SOFT_DECODER.decode(deinterleave(softBits));
        return decoded != null ? extractPayload(decoded) : null;
    }

    /**
     * Extracts the 36-bit payload from the corrected (de-interleaved) message.
     * @param deinterleaved message
     * @return extracted payload with the corrected bit count transferred.
     */
    private static CorrectedBinaryMessage extractPayload(CorrectedBinaryMessage deinterleaved)
    {
        //Extract the message
        CorrectedBinaryMessage extractedMessage = new CorrectedBinaryMessage(36);

        for(int row = 0; row < 3; row++)
        {
            for(int column = 0; column < 12; column++)
            {
                extractedMessage.set(row * 12 + column, deinterleaved.get(row * 17 + column));
            }
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

        for(int index = 0; index < 67; index++)
        {
            deinterleaved[(index * 17) % 67] = interleaved[index];
        }

        deinterleaved[67] = interleaved[67];
        return deinterleaved;
    }

    /**
     * Deinterleaves the transmitted message.
     * @param interleaved message
     * @return deinterleaved message.
     */
    public CorrectedBinaryMessage deinterleave(BinaryMessage interleaved)
    {
        CorrectedBinaryMessage deinterleaved = new CorrectedBinaryMessage(68);

        for(int index = 0; index < 67; index++)
        {
            if(interleaved.get(index))
            {
                deinterleaved.set((index * 17 % 67));
            }
        }

        if(interleaved.get(67))
        {
            deinterleaved.set(67);
        }

        return deinterleaved;
    }

    public static void main(String[] args)
    {
        BPTC_68_36 bptc = new BPTC_68_36();

        String ref = "10101001100001000000001101101111000000110011001110011111101011001100"; //No errors
        String raw = "10101001100001000000001101101111000000110011001110011111101011001100"; //Under test
        CorrectedBinaryMessage rawMessage = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        CorrectedBinaryMessage refMessage = new CorrectedBinaryMessage(BinaryMessage.load(ref));

        bptc.logDiagnostic(rawMessage);

        bptc.correct(rawMessage);

        System.out.println("RAW: " + raw);
        System.out.println("COR: " + rawMessage);
        System.out.println("REF: " + ref);

        rawMessage.xor(refMessage);
        System.out.println("RES: " + rawMessage);

        if(rawMessage.cardinality() > 0)
        {
            System.out.println("--------------------------------");
            System.out.println("Residual Error Map:");
            bptc.logErrorMap(rawMessage);
            System.out.println("--------------------------------");
        }

        System.out.println("\n\n*********** TRUTH DATA ********************");
        rawMessage = new CorrectedBinaryMessage(BinaryMessage.load(raw));
        rawMessage.xor(refMessage);
        System.out.println("XOR: " + rawMessage);
        bptc.logErrorMap(rawMessage);
    }
}
