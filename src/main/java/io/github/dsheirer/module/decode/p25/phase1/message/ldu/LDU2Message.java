/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.phase1.message.ldu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Hamming10;
import io.github.dsheirer.edac.ReedSolomon_24_16_9_P25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class LDU2Message extends LDUMessage
{
    private final static Logger mLog = LoggerFactory.getLogger(LDU2Message.class);

    private static final int[] GOLAY_WORD_STARTS = {288, 298, 308, 318, 472, 482, 492, 502, 656, 666, 676, 686,
        840, 850, 860, 870, 1024, 1034, 1044, 1054, 1208, 1218, 1228, 1238};
    private static final int[] CW_HEX_0 = {288, 289, 290, 291, 292, 293};
    private static final int[] CW_HEX_1 = {298, 299, 300, 301, 302, 303};
    private static final int[] CW_HEX_2 = {308, 309, 310, 311, 312, 313};
    private static final int[] CW_HEX_3 = {318, 319, 320, 321, 322, 323};
    private static final int[] CW_HEX_4 = {472, 473, 474, 475, 475, 477};
    private static final int[] CW_HEX_5 = {482, 483, 484, 485, 486, 487};
    private static final int[] CW_HEX_6 = {492, 493, 494, 495, 496, 497};
    private static final int[] CW_HEX_7 = {502, 503, 504, 505, 506, 507};
    private static final int[] CW_HEX_8 = {656, 657, 658, 659, 660, 661};
    private static final int[] CW_HEX_9 = {666, 667, 668, 669, 670, 671};
    private static final int[] CW_HEX_10 = {676, 677, 678, 679, 680, 681};
    private static final int[] CW_HEX_11 = {686, 686, 688, 689, 690, 691};
    private static final int[] CW_HEX_12 = {840, 841, 842, 843, 844, 845};
    private static final int[] CW_HEX_13 = {850, 851, 852, 853, 854, 855};
    private static final int[] CW_HEX_14 = {860, 861, 862, 863, 864, 865};
    private static final int[] CW_HEX_15 = {870, 871, 872, 873, 874, 875};
    private static final int[] RS_HEX_0 = {1024, 1025, 1026, 1027, 1028, 1029};
    private static final int[] RS_HEX_1 = {1034, 1035, 1036, 1037, 1038, 1039};
    private static final int[] RS_HEX_2 = {1044, 1045, 1046, 1047, 1048, 1049};
    private static final int[] RS_HEX_3 = {1054, 1055, 1056, 1057, 1058, 1059};
    private static final int[] RS_HEX_4 = {1208, 1209, 1210, 1211, 1212, 1213};
    private static final int[] RS_HEX_5 = {1218, 1219, 1220, 1221, 1222, 1223};
    private static final int[] RS_HEX_6 = {1228, 1229, 1230, 1231, 1232, 1233};
    private static final int[] RS_HEX_7 = {1238, 1239, 1240, 1241, 1242, 1243};

    //Reed-Solomon(24,16,9) code protects the encryption sync word.  Maximum correctable errors are floor(9/2) = 4
    private static final ReedSolomon_24_16_9_P25 REED_SOLOMON_24_16_9_P25 = new ReedSolomon_24_16_9_P25();

    private EncryptionSyncParameters mEncryptionSyncParameters;
    private List<Identifier> mIdentifiers;

    public LDU2Message(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.LOGICAL_LINK_DATA_UNIT_2;
    }

    /**
     * Encryption sync parameters allow a user to join an encrypted audio call.
     */
    public EncryptionSyncParameters getEncryptionSyncParameters()
    {
        if(mEncryptionSyncParameters == null)
        {
            createEncryptionSyncParameters();
        }

        return mEncryptionSyncParameters;
    }

    /**
     * Performs error detection and correction against the encryption sync parameters portion of the message and
     * extracts that message into an encryption sync parameters instance.
     */
    private void createEncryptionSyncParameters()
    {
        /* Hamming( 10,6,3 ) error detection and correction */
        for(int index : GOLAY_WORD_STARTS)
        {
            int errors = Hamming10.checkAndCorrect(getMessage(), index);
        }

        /* Reed-Solomon( 24,16,9 ) error detection and correction
         * Check the Reed-Solomon parity bits. The RS decoder expects the code
         * words and reed solomon parity hex codewords in reverse order.
         *
         * Since this is a truncated RS(63) codes, we pad the code with zeros */
        int[] input = new int[63];
        int[] output = new int[63];

        input[0] = getMessage().getInt(RS_HEX_7);
        input[1] = getMessage().getInt(RS_HEX_6);
        input[2] = getMessage().getInt(RS_HEX_5);
        input[3] = getMessage().getInt(RS_HEX_4);
        input[4] = getMessage().getInt(RS_HEX_3);
        input[5] = getMessage().getInt(RS_HEX_2);
        input[6] = getMessage().getInt(RS_HEX_1);
        input[7] = getMessage().getInt(RS_HEX_0);

        input[8] = getMessage().getInt(CW_HEX_15);
        input[9] = getMessage().getInt(CW_HEX_14);
        input[10] = getMessage().getInt(CW_HEX_13);
        input[11] = getMessage().getInt(CW_HEX_12);
        input[12] = getMessage().getInt(CW_HEX_11);
        input[13] = getMessage().getInt(CW_HEX_10);
        input[14] = getMessage().getInt(CW_HEX_9);
        input[15] = getMessage().getInt(CW_HEX_8);
        input[16] = getMessage().getInt(CW_HEX_7);
        input[17] = getMessage().getInt(CW_HEX_6);
        input[18] = getMessage().getInt(CW_HEX_5);
        input[19] = getMessage().getInt(CW_HEX_4);
        input[20] = getMessage().getInt(CW_HEX_3);
        input[21] = getMessage().getInt(CW_HEX_2);
        input[22] = getMessage().getInt(CW_HEX_1);
        input[23] = getMessage().getInt(CW_HEX_0);
        /* indexes 24 - 62 are defaulted to zero */

        boolean irrecoverableErrors = REED_SOLOMON_24_16_9_P25.decode(input, output);

        BinaryMessage binaryMessage = new BinaryMessage(96);

        int pointer = 0;

        for(int x = 23; x >= 8; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mEncryptionSyncParameters = new EncryptionSyncParameters(binaryMessage);

        if(irrecoverableErrors)
        {
            mEncryptionSyncParameters.setValid(false);
        }
        else
        {
            //If we corrected any bit errors, update the original message with the bit error count
            for(int x = 0; x < 23; x++)
            {
                if(output[x] != input[x])
                {
                    getMessage().incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
                }
            }
        }
    }

    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getNAC());

            if(getEncryptionSyncParameters().isValid())
            {
                mIdentifiers.addAll(getEncryptionSyncParameters().getIdentifiers());
            }
        }

        return mIdentifiers;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ").append(getEncryptionSyncParameters());

        return sb.toString();
    }
}
