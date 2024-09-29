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
package io.github.dsheirer.module.decode.p25.phase1.message.hdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay18;
import io.github.dsheirer.edac.ReedSolomon_63_47_17_P25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HDUMessage extends P25P1Message
{
    private final static Logger mLog = LoggerFactory.getLogger(HDUMessage.class);

    private static final int[] GOLAY_WORD_STARTS = {0, 18, 36, 54, 72, 90, 108, 126, 144, 162, 180, 198, 216, 234, 252,
        270, 288, 306, 324, 342, 360, 278, 396, 414, 432, 450, 468, 486, 504, 522, 540, 558, 576, 594, 612, 630};

    private static final int[] CW_HEX_0 = {0, 1, 2, 3, 4, 5};
    private static final int[] CW_HEX_1 = {18, 19, 20, 21, 22, 23};
    private static final int[] CW_HEX_2 = {36, 37, 38, 39, 40, 41};
    private static final int[] CW_HEX_3 = {54, 55, 56, 57, 58, 59};
    private static final int[] CW_HEX_4 = {72, 73, 74, 75, 76, 77};
    private static final int[] CW_HEX_5 = {90, 91, 92, 93, 94, 95};
    private static final int[] CW_HEX_6 = {108, 109, 110, 111, 112, 113};
    private static final int[] CW_HEX_7 = {126, 127, 128, 129, 130, 131};
    private static final int[] CW_HEX_8 = {144, 145, 146, 147, 148, 149};
    private static final int[] CW_HEX_9 = {162, 163, 164, 165, 166, 167};
    private static final int[] CW_HEX_10 = {180, 181, 182, 183, 184, 185};
    private static final int[] CW_HEX_11 = {198, 199, 200, 201, 202, 203};
    private static final int[] CW_HEX_12 = {216, 217, 218, 219, 220, 221};
    private static final int[] CW_HEX_13 = {234, 235, 236, 237, 238, 239};
    private static final int[] CW_HEX_14 = {252, 253, 254, 255, 256, 257};
    private static final int[] CW_HEX_15 = {270, 271, 272, 273, 274, 275};
    private static final int[] CW_HEX_16 = {288, 289, 290, 291, 292, 293};
    private static final int[] CW_HEX_17 = {306, 307, 308, 309, 310, 311};
    private static final int[] CW_HEX_18 = {324, 325, 326, 327, 328, 329};
    private static final int[] CW_HEX_19 = {342, 343, 344, 345, 346, 347};
    private static final int[] RS_HEX_0 = {360, 361, 362, 363, 364, 365};
    private static final int[] RS_HEX_1 = {378, 379, 380, 381, 382, 383};
    private static final int[] RS_HEX_2 = {396, 397, 398, 399, 400, 401};
    private static final int[] RS_HEX_3 = {414, 415, 416, 417, 418, 419};
    private static final int[] RS_HEX_4 = {432, 433, 434, 435, 436, 437};
    private static final int[] RS_HEX_5 = {450, 451, 452, 453, 454, 455};
    private static final int[] RS_HEX_6 = {468, 469, 470, 471, 472, 473};
    private static final int[] RS_HEX_7 = {486, 487, 488, 489, 490, 491};
    private static final int[] RS_HEX_8 = {504, 505, 506, 507, 508, 509};
    private static final int[] RS_HEX_9 = {522, 523, 524, 525, 526, 527};
    private static final int[] RS_HEX_10 = {540, 541, 542, 543, 544, 545};
    private static final int[] RS_HEX_11 = {558, 559, 560, 561, 562, 563};
    private static final int[] RS_HEX_12 = {576, 577, 578, 579, 580, 581};
    private static final int[] RS_HEX_13 = {594, 595, 596, 597, 598, 599};
    private static final int[] RS_HEX_14 = {612, 613, 614, 615, 616, 617};
    private static final int[] RS_HEX_15 = {630, 631, 632, 633, 634, 635};

    private static final ReedSolomon_63_47_17_P25 reedSolomon_63_47_17 = new ReedSolomon_63_47_17_P25();

    private HeaderData mHeaderData;

    public HDUMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.HEADER_DATA_UNIT;
    }

    public HeaderData getHeaderData()
    {
        if(mHeaderData == null)
        {
            extractHeaderData();
        }

        return mHeaderData;
    }

    /**
     * Performs error detection and correction and extracts the header data from the HDU message
     */
    private void extractHeaderData()
    {
        for(int index : GOLAY_WORD_STARTS)
        {
            Golay18.checkAndCorrect(getMessage(), index);
        }

        /* Reed-Solomon( 36,20,17 ) error detection and correction
         * Check the Reed-Solomon parity bits. The RS decoder expects the link
         * control data and reed solomon parity hex codewords in reverse order.
         * The RS(24,12,13) code used by P25 removes the left-hand 47 data hex
         * words, so we replace them with zeros. */
        int[] input = new int[63];
        int[] output = new int[63];

        input[0] = getMessage().getInt(RS_HEX_15);
        input[1] = getMessage().getInt(RS_HEX_14);
        input[2] = getMessage().getInt(RS_HEX_13);
        input[3] = getMessage().getInt(RS_HEX_12);
        input[4] = getMessage().getInt(RS_HEX_11);
        input[5] = getMessage().getInt(RS_HEX_10);
        input[6] = getMessage().getInt(RS_HEX_9);
        input[7] = getMessage().getInt(RS_HEX_8);
        input[8] = getMessage().getInt(RS_HEX_7);
        input[9] = getMessage().getInt(RS_HEX_6);
        input[10] = getMessage().getInt(RS_HEX_5);
        input[11] = getMessage().getInt(RS_HEX_4);
        input[12] = getMessage().getInt(RS_HEX_3);
        input[13] = getMessage().getInt(RS_HEX_2);
        input[14] = getMessage().getInt(RS_HEX_1);
        input[15] = getMessage().getInt(RS_HEX_0);

        input[16] = getMessage().getInt(CW_HEX_19);
        input[17] = getMessage().getInt(CW_HEX_18);
        input[18] = getMessage().getInt(CW_HEX_17);
        input[19] = getMessage().getInt(CW_HEX_16);
        input[20] = getMessage().getInt(CW_HEX_15);
        input[21] = getMessage().getInt(CW_HEX_14);
        input[22] = getMessage().getInt(CW_HEX_13);
        input[23] = getMessage().getInt(CW_HEX_12);
        input[24] = getMessage().getInt(CW_HEX_11);
        input[25] = getMessage().getInt(CW_HEX_10);
        input[26] = getMessage().getInt(CW_HEX_9);
        input[27] = getMessage().getInt(CW_HEX_8);
        input[28] = getMessage().getInt(CW_HEX_7);
        input[29] = getMessage().getInt(CW_HEX_6);
        input[30] = getMessage().getInt(CW_HEX_5);
        input[31] = getMessage().getInt(CW_HEX_4);
        input[32] = getMessage().getInt(CW_HEX_3);
        input[33] = getMessage().getInt(CW_HEX_2);
        input[34] = getMessage().getInt(CW_HEX_1);
        input[35] = getMessage().getInt(CW_HEX_0);
        /* indexes 36 - 62 are defaulted to zero */

        boolean irrecoverableErrors;

        try
        {
            irrecoverableErrors = reedSolomon_63_47_17.decode(input, output);
        }
        catch(Exception e)
        {
            irrecoverableErrors = true;
        }

        BinaryMessage binaryMessage = new BinaryMessage(120);

        int pointer = 0;

        for(int x = 35; x >= 16; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mHeaderData = new HeaderData(binaryMessage);

        if(irrecoverableErrors)
        {
            //Set the header data as invalid
            mHeaderData.setValid(false);

            //Est the whole HDU message as invalid.
            setValid(false);
        }
        else
        {
            //If we corrected any bit errors, update the original message with the bit error count
            for(int x = 0; x <= 35; x++)
            {
                if(output[x] != input[x])
                {
                    getMessage().incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
                }
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NAC:").append(getNAC());
        sb.append(" HDU   ").append(getHeaderData());
        return sb.toString();
    }

    public List<Identifier> getIdentifiers()
    {
        return getHeaderData().getIdentifiers();
    }
}
