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

/**
 * Block product turbo code (BPTC) for decoding Full Link Control messages reassembled from the 32-bit payload fragments
 * in each of the Voice B-E messages.
 */
public class BPTC_128_77 extends BPTCBase
{
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
        CorrectedBinaryMessage deinterleaved = deinterleave(message);
        correct(deinterleaved);

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
