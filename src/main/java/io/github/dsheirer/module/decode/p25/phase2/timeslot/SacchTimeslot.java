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

package io.github.dsheirer.module.decode.p25.phase2.timeslot;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import java.util.List;

/**
 * Slow Associated Control CHannel (SACCH) timeslot carrying an I-OEMI message
 */
public class SacchTimeslot extends AbstractSignalingTimeslot
{
    private static final int MAX_OCTET_INDEX = 168; //180-12 = message length minus CRC-12 checksum.

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
    private static final IntField INFO_12 = IntField.range(68, 73); //Gap for duid 114-115
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
    private static final IntField INFO_23 = IntField.range(136, 141);
    private static final IntField INFO_24 = IntField.range(142, 147);
    private static final IntField INFO_25 = IntField.range(148, 153);
    private static final IntField INFO_26 = IntField.range(154, 159);
    private static final IntField INFO_27 = IntField.range(160, 165);
    private static final IntField INFO_28 = IntField.range(166, 171);
    private static final IntField INFO_29 = IntField.range(172, 177);
    private static final IntField INFO_30 = IntField.range(178, 183);
    private static final IntField PARITY_1 = IntField.range(184, 189);
    private static final IntField PARITY_2 = IntField.range(190, 195);
    private static final IntField PARITY_3 = IntField.range(196, 201);
    private static final IntField PARITY_4 = IntField.range(202, 207);
    private static final IntField PARITY_5 = IntField.range(208, 213);
    private static final IntField PARITY_6 = IntField.range(214, 219);
    private static final IntField PARITY_7 = IntField.range(220, 225);
    private static final IntField PARITY_8 = IntField.range(226, 231);
    private static final IntField PARITY_9 = IntField.range(232, 237);
    private static final IntField PARITY_10 = IntField.range(238, 243); //Gap for duid 284-285
    private static final IntField PARITY_11 = IntField.range(246, 251);
    private static final IntField PARITY_12 = IntField.range(252, 257);
    private static final IntField PARITY_13 = IntField.range(258, 263);
    private static final IntField PARITY_14 = IntField.range(264, 269);
    private static final IntField PARITY_15 = IntField.range(270, 275);
    private static final IntField PARITY_16 = IntField.range(276, 281);
    private static final IntField PARITY_17 = IntField.range(282, 287);
    private static final IntField PARITY_18 = IntField.range(288, 293);
    private static final IntField PARITY_19 = IntField.range(294, 299);
    private static final IntField PARITY_20 = IntField.range(300, 305);
    private static final IntField PARITY_21 = IntField.range(306, 311);
    private static final IntField PARITY_22 = IntField.range(312, 317);

    private List<MacMessage> mMacMessages;

    /**
     * Constructs a scrambled SACCH timeslot
     *
     * @param message containing 320 scrambled bits for the timeslot
     * @param scramblingSequence to descramble the message
     * @param timeslot of the message
     * @param timestamp of the message
     */
    public SacchTimeslot(CorrectedBinaryMessage message, BinaryMessage scramblingSequence, int timeslot,
                         long timestamp)
    {
        super(message, DataUnitID.SCRAMBLED_SACCH, scramblingSequence, timeslot, timestamp);
    }

    /**
     * Constructs an un-scrambled SACCH timeslot
     *
     * @param message containing 320 scrambled bits for the timeslot
     * @param timeslot of the message
     * @param timestamp of the message
     */
    public SacchTimeslot(CorrectedBinaryMessage message, int timeslot, long timestamp)
    {
        super(message, DataUnitID.UNSCRAMBLED_SACCH, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());

        if(getDataUnitID() == DataUnitID.UNSCRAMBLED_SACCH)
        {
            sb.append(" SA-UN");
            sb.append(" ").append(getMacMessages().toString());
        }
        else
        {
            sb.append(" SA-SC");
            sb.append(" ").append(getMacMessages().toString());
        }


        return sb.toString();
    }

    /**
     * Information Outbound Encoded MAC Information (I-OEMI) message(s) carried by this timeslot
     */
    @Override
    public List<MacMessage> getMacMessages()
    {
        if(mMacMessages == null)
        {
            int[] input = new int[63];
            float[] reliabilities = createReliabilities();

//            input[0] = 0; //Punctured
//            input[1] = 0; //Punctured
//            input[2] = 0; //Punctured
//            input[3] = 0; //Punctured
//            input[4] = 0; //Punctured
//            input[5] = 0; //Punctured
            load(input, reliabilities, 6, PARITY_22);
            load(input, reliabilities, 7, PARITY_21);
            load(input, reliabilities, 8, PARITY_20);
            load(input, reliabilities, 9, PARITY_19);
            load(input, reliabilities, 10, PARITY_18);
            load(input, reliabilities, 11, PARITY_17);
            load(input, reliabilities, 12, PARITY_16);
            load(input, reliabilities, 13, PARITY_15);
            load(input, reliabilities, 14, PARITY_14);
            load(input, reliabilities, 15, PARITY_13);
            load(input, reliabilities, 16, PARITY_12);
            load(input, reliabilities, 17, PARITY_11);
            load(input, reliabilities, 18, PARITY_10);
            load(input, reliabilities, 19, PARITY_9);
            load(input, reliabilities, 20, PARITY_8);
            load(input, reliabilities, 21, PARITY_7);
            load(input, reliabilities, 22, PARITY_6);
            load(input, reliabilities, 23, PARITY_5);
            load(input, reliabilities, 24, PARITY_4);
            load(input, reliabilities, 25, PARITY_3);
            load(input, reliabilities, 26, PARITY_2);
            load(input, reliabilities, 27, PARITY_1);
            load(input, reliabilities, 28, INFO_30);
            load(input, reliabilities, 29, INFO_29);
            load(input, reliabilities, 30, INFO_28);
            load(input, reliabilities, 31, INFO_27);
            load(input, reliabilities, 32, INFO_26);
            load(input, reliabilities, 33, INFO_25);
            load(input, reliabilities, 34, INFO_24);
            load(input, reliabilities, 35, INFO_23);
            load(input, reliabilities, 36, INFO_22);
            load(input, reliabilities, 37, INFO_21);
            load(input, reliabilities, 38, INFO_20);
            load(input, reliabilities, 39, INFO_19);
            load(input, reliabilities, 40, INFO_18);
            load(input, reliabilities, 41, INFO_17);
            load(input, reliabilities, 42, INFO_16);
            load(input, reliabilities, 43, INFO_15);
            load(input, reliabilities, 44, INFO_14);
            load(input, reliabilities, 45, INFO_13);
            load(input, reliabilities, 46, INFO_12);
            load(input, reliabilities, 47, INFO_11);
            load(input, reliabilities, 48, INFO_10);
            load(input, reliabilities, 49, INFO_9);
            load(input, reliabilities, 50, INFO_8);
            load(input, reliabilities, 51, INFO_7);
            load(input, reliabilities, 52, INFO_6);
            load(input, reliabilities, 53, INFO_5);
            load(input, reliabilities, 54, INFO_4);
            load(input, reliabilities, 55, INFO_3);
            load(input, reliabilities, 56, INFO_2);
            load(input, reliabilities, 57, INFO_1);
//            input[58] = 0; //Shortened
//            input[59] = 0; //Shortened
//            input[60] = 0; //Shortened
//            input[61] = 0; //Shortened
//            input[62] = 0; //Shortened

            mMacMessages = decode(input, reliabilities, 6, 57, MAX_OCTET_INDEX);
        }

        return mMacMessages;
    }
}
