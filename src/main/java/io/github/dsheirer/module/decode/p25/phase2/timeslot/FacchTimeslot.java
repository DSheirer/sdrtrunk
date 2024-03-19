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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.edac.ReedSolomon_63_35_29_P25;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessageFactory;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureFailedRS;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fast Associated Control CHannel (FACCH) timeslot carrying a S-OEMI Message
 */
public class FacchTimeslot extends AbstractSignalingTimeslot
{
    private final static Logger mLog = LoggerFactory.getLogger(FacchTimeslot.class);
    private static final int MAX_OCTET_INDEX = 144; //156-12 = message length minus CRC-12 checksum.

    private static final IntField INFO_1 = IntField.range(2, 7);
    private static final IntField INFO_2 = IntField.range(8, 13);
    private static final IntField INFO_3 = IntField.range(14, 19);
    private static final IntField INFO_4 = IntField.range(20, 25);
    private static final IntField INFO_5 = IntField.range(26, 31);
    private static final IntField INFO_6 = IntField.range(32, 37);
    private static final IntField INFO_7 = IntField.range(38, 43);
    private static final IntField INFO_8 = IntField.range(44, 49);
    private static final IntField INFO_9 = IntField.range(50, 55);
    private static final IntField INFO_10 = IntField.range(56, 61);
    private static final IntField INFO_11 = IntField.range(62, 67);
    private static final IntField INFO_12 = IntField.range(68, 73); //Gap for duid 74-75
    private static final IntField INFO_13 = IntField.range(76, 81);
    private static final IntField INFO_14 = IntField.range(82, 87);
    private static final IntField INFO_15 = IntField.range(88, 93);
    private static final IntField INFO_16 = IntField.range(94, 99);
    private static final IntField INFO_17 = IntField.range(100, 105);
    private static final IntField INFO_18 = IntField.range(106, 111);
    private static final IntField INFO_19 = IntField.range(112, 117);
    private static final IntField INFO_20 = IntField.range(118, 123);
    private static final IntField INFO_21 = IntField.range(124, 129);
    private static final IntField INFO_22 = IntField.range(130, 135);
    private static final FragmentedIntField INFO_23 = FragmentedIntField.of(136, 137, 180, 181, 182, 183); //Gap for sync 138-179
    private static final IntField INFO_24 = IntField.range(184, 189);
    private static final IntField INFO_25 = IntField.range(190, 195);
    private static final IntField INFO_26 = IntField.range(196, 201);
    private static final IntField PARITY_1 = IntField.range(202, 207);
    private static final IntField PARITY_2 = IntField.range(208, 213);
    private static final IntField PARITY_3 = IntField.range(214, 219);
    private static final IntField PARITY_4 = IntField.range(220, 225);
    private static final IntField PARITY_5 = IntField.range(226, 231);
    private static final IntField PARITY_6 = IntField.range(232, 237);
    private static final IntField PARITY_7 = IntField.range(238, 243); //Gap for duid 244-245
    private static final IntField PARITY_8 = IntField.range(246, 251);
    private static final IntField PARITY_9 = IntField.range(252, 257);
    private static final IntField PARITY_10 = IntField.range(258, 263);
    private static final IntField PARITY_11 = IntField.range(264, 269);
    private static final IntField PARITY_12 = IntField.range(270, 275);
    private static final IntField PARITY_13 = IntField.range(276, 281);
    private static final IntField PARITY_14 = IntField.range(282, 287);
    private static final IntField PARITY_15 = IntField.range(288, 293);
    private static final IntField PARITY_16 = IntField.range(294, 299);
    private static final IntField PARITY_17 = IntField.range(300, 305);
    private static final IntField PARITY_18 = IntField.range(306, 311);
    private static final IntField PARITY_19 = IntField.range(312, 317);

    private List<MacMessage> mMacMessages;

    /**
     * Constructs a scrambled FACCH timeslot
     * @param message containing 320 scrambled bits for the timeslot
     * @param scramblingSequence to descramble the message
     */
    public FacchTimeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence, int timeslot, long timestamp)
    {
        super(message, DataUnitID.SCRAMBLED_FACCH, scramblingSequence, timeslot, timestamp);
    }

    /**
     * Constructs a un-scrambled FACCH timeslot
     * @param message containing 320 scrambled bits for the timeslot
     */
    public FacchTimeslot(CorrectedBinaryMessage message, int timeslot, long timestamp)
    {
        super(message, DataUnitID.UNSCRAMBLED_FACCH, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());

        if(getDataUnitID() == DataUnitID.UNSCRAMBLED_FACCH)
        {
            sb.append(" FA-UN");
            sb.append(" ").append(getMacMessages().toString());
        }
        else
        {
            sb.append(" FA-SC");
            sb.append(" SCRAMBLED:").append(getMessage().toHexString());
        }


        return sb.toString();
    }

    /**
     * Signaling Outbound Encoded MAC Information (S-OEMI) message(s) carried by this timeslot
     */
    @Override
    public List<MacMessage> getMacMessages()
    {
        if(mMacMessages == null)
        {
            int[] input = new int[63];
            int[] output = new int[63];

//            input[0] = 0; //Punctured
//            input[1] = 0; //Punctured
//            input[2] = 0; //Punctured
//            input[3] = 0; //Punctured
//            input[4] = 0; //Punctured
//            input[5] = 0; //Punctured
//            input[6] = 0; //Punctured
//            input[7] = 0; //Punctured
//            input[8] = 0; //Punctured
            input[9] = getInt(PARITY_19);
            input[10] = getInt(PARITY_18);
            input[11] = getInt(PARITY_17);
            input[12] = getInt(PARITY_16);
            input[13] = getInt(PARITY_15);
            input[14] = getInt(PARITY_14);
            input[15] = getInt(PARITY_13);
            input[16] = getInt(PARITY_12);
            input[17] = getInt(PARITY_11);
            input[18] = getInt(PARITY_10);
            input[19] = getInt(PARITY_9);
            input[20] = getInt(PARITY_8);
            input[21] = getInt(PARITY_7);
            input[22] = getInt(PARITY_6);
            input[23] = getInt(PARITY_5);
            input[24] = getInt(PARITY_4);
            input[25] = getInt(PARITY_3);
            input[26] = getInt(PARITY_2);
            input[27] = getInt(PARITY_1);
            input[28] = getInt(INFO_26);
            input[29] = getInt(INFO_25);
            input[30] = getInt(INFO_24);
            input[31] = getInt(INFO_23);
            input[32] = getInt(INFO_22);
            input[33] = getInt(INFO_21);
            input[34] = getInt(INFO_20);
            input[35] = getInt(INFO_19);
            input[36] = getInt(INFO_18);
            input[37] = getInt(INFO_17);
            input[38] = getInt(INFO_16);
            input[39] = getInt(INFO_15);
            input[40] = getInt(INFO_14);
            input[41] = getInt(INFO_13);
            input[42] = getInt(INFO_12);
            input[43] = getInt(INFO_11);
            input[44] = getInt(INFO_10);
            input[45] = getInt(INFO_9);
            input[46] = getInt(INFO_8);
            input[47] = getInt(INFO_7);
            input[48] = getInt(INFO_6);
            input[49] = getInt(INFO_5);
            input[50] = getInt(INFO_4);
            input[51] = getInt(INFO_3);
            input[52] = getInt(INFO_2);
            input[53] = getInt(INFO_1);
//            input[54] = 0; //Shortened
//            input[55] = 0; //Shortened
//            input[56] = 0; //Shortened
//            input[57] = 0; //Shortened
//            input[58] = 0; //Shortened
//            input[59] = 0; //Shortened
//            input[60] = 0; //Shortened
//            input[61] = 0; //Shortened
//            input[62] = 0; //Shortened

            ReedSolomon_63_35_29_P25 reedSolomon_63_35_29 = new ReedSolomon_63_35_29_P25();

            boolean irrecoverableErrors;

            try
            {
                irrecoverableErrors = reedSolomon_63_35_29.decode(input, output);
            }
            catch(Exception e)
            {
                mLog.error("Error", e);
                irrecoverableErrors = true;
            }

            CorrectedBinaryMessage binaryMessage = new CorrectedBinaryMessage(156);

            int pointer = 0;

            for(int x = 53; x >= 28; x--)
            {
                if(output[x] != -1)
                {
                    binaryMessage.load(pointer, 6, output[x]);
                }

                pointer += 6;
            }

            if(irrecoverableErrors)
            {
                MacMessage macMessage = new MacMessage(getTimeslot(), getDataUnitID(), binaryMessage, getTimestamp(),
                        new MacStructureFailedRS(binaryMessage, 0));
                macMessage.setValid(false);
                mMacMessages = new ArrayList<>();
                mMacMessages.add(macMessage);
            }
            else
            {
                //If we corrected any bit errors, update the original message with the bit error count
                for(int x = 9; x <= 53; x++)
                {
                    if(output[x] != input[x])
                    {
                        binaryMessage.incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
                    }
                }
            }

            mMacMessages = MacMessageFactory.create(getTimeslot(), getDataUnitID(), binaryMessage, getTimestamp(),
                    MAX_OCTET_INDEX);
        }

        return mMacMessages;
    }
}
