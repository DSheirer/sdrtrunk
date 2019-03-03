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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.ReedSolomon_63_35_29;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessageFactory;

/**
 * Slow Associated Control CHannel (SACCH) timeslot carrying an I-OEMI message
 */
public class SacchTimeslot extends AbstractSignalingTimeslot
{
    private static final int[] INFO_1 = {42, 43, 44, 45, 46, 47};
    private static final int[] INFO_2 = {48, 49, 50, 51, 52, 53};
    private static final int[] INFO_3 = {54, 55, 56, 57, 58, 59};
    private static final int[] INFO_4 = {60, 61, 62, 63, 64, 65};
    private static final int[] INFO_5 = {66, 67, 68, 69, 70, 71};
    private static final int[] INFO_6 = {72, 73, 74, 75, 76, 77};
    private static final int[] INFO_7 = {78, 79, 80, 81, 82, 83};
    private static final int[] INFO_8 = {84, 85, 86, 87, 88, 89};
    private static final int[] INFO_9 = {90, 91, 92, 93, 94, 95};
    private static final int[] INFO_10 = {96, 97, 98, 99, 100, 101};
    private static final int[] INFO_11 = {102, 103, 104, 105, 106, 107};
    private static final int[] INFO_12 = {108, 109, 110, 111, 112, 113}; //Gap for duid 114-115
    private static final int[] INFO_13 = {116, 117, 118, 119, 120, 121};
    private static final int[] INFO_14 = {122, 123, 124, 125, 126, 127};
    private static final int[] INFO_15 = {128, 129, 130, 131, 132, 133};
    private static final int[] INFO_16 = {134, 135, 136, 137, 138, 139};
    private static final int[] INFO_17 = {140, 141, 142, 143, 144, 145};
    private static final int[] INFO_18 = {146, 147, 148, 149, 150, 151};
    private static final int[] INFO_19 = {152, 153, 154, 155, 156, 157};
    private static final int[] INFO_20 = {158, 159, 160, 161, 162, 163};
    private static final int[] INFO_21 = {164, 165, 166, 167, 168, 169};
    private static final int[] INFO_22 = {170, 171, 172, 173, 174, 175};
    private static final int[] INFO_23 = {176, 177, 178, 179, 180, 181};
    private static final int[] INFO_24 = {182, 183, 184, 185, 186, 187};
    private static final int[] INFO_25 = {188, 189, 190, 191, 192, 193};
    private static final int[] INFO_26 = {194, 195, 196, 197, 198, 199};
    private static final int[] INFO_27 = {200, 201, 202, 203, 204, 205};
    private static final int[] INFO_28 = {206, 207, 208, 209, 210, 211};
    private static final int[] INFO_29 = {212, 213, 214, 215, 216, 217};
    private static final int[] INFO_30 = {218, 219, 220, 221, 222, 223};
    private static final int[] PARITY_1 = {224, 225, 226, 227, 228, 229};
    private static final int[] PARITY_2 = {230, 231, 232, 233, 234, 235};
    private static final int[] PARITY_3 = {236, 237, 238, 239, 240, 241};
    private static final int[] PARITY_4 = {242, 243, 244, 245, 246, 247};
    private static final int[] PARITY_5 = {248, 249, 250, 251, 252, 253};
    private static final int[] PARITY_6 = {254, 255, 256, 257, 258, 259};
    private static final int[] PARITY_7 = {260, 261, 262, 263, 264, 265};
    private static final int[] PARITY_8 = {266, 267, 268, 269, 270, 271};
    private static final int[] PARITY_9 = {272, 273, 274, 275, 276, 277};
    private static final int[] PARITY_10 = {278, 279, 280, 281, 282, 283}; //Gap for duid 284-285
    private static final int[] PARITY_11 = {286, 287, 288, 289, 290, 291};
    private static final int[] PARITY_12 = {292, 293, 294, 295, 296, 297};
    private static final int[] PARITY_13 = {298, 299, 300, 301, 302, 303};
    private static final int[] PARITY_14 = {304, 305, 306, 307, 308, 309};
    private static final int[] PARITY_15 = {310, 311, 312, 313, 314, 315};
    private static final int[] PARITY_16 = {316, 317, 318, 319, 320, 321};
    private static final int[] PARITY_17 = {322, 323, 324, 325, 326, 327};
    private static final int[] PARITY_18 = {328, 329, 330, 331, 332, 333};
    private static final int[] PARITY_19 = {334, 335, 336, 337, 338, 339};
    private static final int[] PARITY_20 = {340, 341, 342, 343, 344, 345};
    private static final int[] PARITY_21 = {346, 347, 348, 349, 350, 351};
    private static final int[] PARITY_22 = {352, 353, 354, 355, 356, 357};

    private MacMessage mMacMessage;

    /**
     * Constructs a scrambled SACCH timeslot
     *
     * @param message containing 320 scrambled bits for the timeslot
     * @param scramblingSequence to descramble the message
     */
    public SacchTimeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence)
    {
        super(message, DataUnitID.SCRAMBLED_SACCH, scramblingSequence);
    }

    /**
     * Constructs an un-scrambled SACCH timeslot
     *
     * @param message containing 320 scrambled bits for the timeslot
     */
    public SacchTimeslot(CorrectedBinaryMessage message)
    {
        super(message, DataUnitID.UNSCRAMBLED_SACCH);
    }

    /**
     * Information Outbound Encoded MAC Information (I-OEMI) message carried by this timeslot
     */
    @Override
    public MacMessage getMacMessage()
    {
        if(mMacMessage == null)
        {
            int[] input = new int[63];
            int[] output = new int[63];

//            input[0] = 0; //Punctured
//            input[1] = 0; //Punctured
//            input[2] = 0; //Punctured
//            input[3] = 0; //Punctured
//            input[4] = 0; //Punctured
//            input[5] = 0; //Punctured
            input[6] = getMessage().getInt(PARITY_22);
            input[7] = getMessage().getInt(PARITY_21);
            input[8] = getMessage().getInt(PARITY_20);
            input[9] = getMessage().getInt(PARITY_19);
            input[10] = getMessage().getInt(PARITY_18);
            input[11] = getMessage().getInt(PARITY_17);
            input[12] = getMessage().getInt(PARITY_16);
            input[13] = getMessage().getInt(PARITY_15);
            input[14] = getMessage().getInt(PARITY_14);
            input[15] = getMessage().getInt(PARITY_13);
            input[16] = getMessage().getInt(PARITY_12);
            input[17] = getMessage().getInt(PARITY_11);
            input[18] = getMessage().getInt(PARITY_10);
            input[19] = getMessage().getInt(PARITY_9);
            input[20] = getMessage().getInt(PARITY_8);
            input[21] = getMessage().getInt(PARITY_7);
            input[22] = getMessage().getInt(PARITY_6);
            input[23] = getMessage().getInt(PARITY_5);
            input[24] = getMessage().getInt(PARITY_4);
            input[25] = getMessage().getInt(PARITY_3);
            input[26] = getMessage().getInt(PARITY_2);
            input[27] = getMessage().getInt(PARITY_1);
            input[28] = getMessage().getInt(INFO_30);
            input[29] = getMessage().getInt(INFO_29);
            input[30] = getMessage().getInt(INFO_28);
            input[31] = getMessage().getInt(INFO_27);
            input[32] = getMessage().getInt(INFO_26);
            input[33] = getMessage().getInt(INFO_25);
            input[34] = getMessage().getInt(INFO_24);
            input[35] = getMessage().getInt(INFO_23);
            input[36] = getMessage().getInt(INFO_22);
            input[37] = getMessage().getInt(INFO_21);
            input[38] = getMessage().getInt(INFO_20);
            input[39] = getMessage().getInt(INFO_19);
            input[40] = getMessage().getInt(INFO_18);
            input[41] = getMessage().getInt(INFO_17);
            input[42] = getMessage().getInt(INFO_16);
            input[43] = getMessage().getInt(INFO_15);
            input[44] = getMessage().getInt(INFO_14);
            input[45] = getMessage().getInt(INFO_13);
            input[46] = getMessage().getInt(INFO_12);
            input[47] = getMessage().getInt(INFO_11);
            input[48] = getMessage().getInt(INFO_10);
            input[49] = getMessage().getInt(INFO_9);
            input[50] = getMessage().getInt(INFO_8);
            input[51] = getMessage().getInt(INFO_7);
            input[52] = getMessage().getInt(INFO_6);
            input[53] = getMessage().getInt(INFO_5);
            input[54] = getMessage().getInt(INFO_4);
            input[55] = getMessage().getInt(INFO_3);
            input[56] = getMessage().getInt(INFO_2);
            input[57] = getMessage().getInt(INFO_1);
//            input[58] = 0; //Shortened
//            input[59] = 0; //Shortened
//            input[60] = 0; //Shortened
//            input[61] = 0; //Shortened
//            input[62] = 0; //Shortened

            //Reed-Solomon(52,30,23) code protects the IOEMI word.  Maximum correctable errors are: 14 (58 - 30 / 2)
            ReedSolomon_63_35_29 reedSolomon_63_35_29 = new ReedSolomon_63_35_29(14);

            boolean irrecoverableErrors;

            try
            {
                irrecoverableErrors = reedSolomon_63_35_29.decode(input, output);
            }
            catch(Exception e)
            {
                irrecoverableErrors = true;
            }

            CorrectedBinaryMessage binaryMessage = new CorrectedBinaryMessage(180);

            int pointer = 0;

            for(int x = 57; x >= 28; x--)
            {
                if(output[x] != -1)
                {
                    binaryMessage.load(pointer, 6, output[x]);
                }

                pointer += 6;
            }

            mMacMessage = MacMessageFactory.create(binaryMessage);

            if(irrecoverableErrors)
            {
                mMacMessage.setValid(false);
            }
            else
            {
                //If we corrected any bit errors, update the original message with the bit error count
                for(int x = 0; x <= 57; x++)
                {
                    if(output[x] != input[x])
                    {
                        binaryMessage.incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
                    }
                }
            }

        }

        return mMacMessage;
    }
}
