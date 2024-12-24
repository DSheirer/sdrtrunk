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

/**
 * Block product turbo code (BPTC) for decoding Short Link Control reassembled from CACH fragments.
 */
public class BPTC_68_36 extends BPTCBase
{
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
