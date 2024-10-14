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
package io.github.dsheirer.module.decode.p25.phase1.message.tdu;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.edac.ReedSolomon_24_12_13_P25;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.phase1.P25P1DataUnitID;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWordFactory;
import java.util.Collections;
import java.util.List;

public class TDULCMessage extends P25P1Message implements IFrequencyBandReceiver
{
    public static final int[] LC_HEX_0 = {0, 1, 2, 3, 4, 5};
    public static final int[] LC_HEX_1 = {6, 7, 8, 9, 10, 11};
    public static final int[] LC_HEX_2 = {24, 25, 26, 27, 28, 29};
    public static final int[] LC_HEX_3 = {30, 31, 32, 33, 34, 35};
    public static final int[] LC_HEX_4 = {48, 49, 50, 51, 52, 53};
    public static final int[] LC_HEX_5 = {54, 55, 56, 57, 58, 59};
    public static final int[] LC_HEX_6 = {72, 73, 74, 75, 76, 77};
    public static final int[] LC_HEX_7 = {78, 79, 80, 81, 82, 83};
    public static final int[] LC_HEX_8 = {96, 97, 98, 99, 100, 101};
    public static final int[] LC_HEX_9 = {102, 103, 104, 105, 106, 107};
    public static final int[] LC_HEX_10 = {120, 121, 122, 123, 124, 125};
    public static final int[] LC_HEX_11 = {126, 127, 128, 129, 130, 131};
    public static final int[] RS_HEX_0 = {144, 145, 146, 147, 148, 149};
    public static final int[] RS_HEX_1 = {150, 151, 152, 153, 154, 155};
    public static final int[] RS_HEX_2 = {168, 169, 170, 171, 172, 173};
    public static final int[] RS_HEX_3 = {174, 175, 176, 177, 178, 179};
    public static final int[] RS_HEX_4 = {192, 193, 194, 195, 196, 197};
    public static final int[] RS_HEX_5 = {198, 199, 200, 201, 202, 203};
    public static final int[] RS_HEX_6 = {216, 217, 218, 219, 220, 221};
    public static final int[] RS_HEX_7 = {222, 223, 224, 225, 226, 227};
    public static final int[] RS_HEX_8 = {240, 241, 242, 243, 244, 245};
    public static final int[] RS_HEX_9 = {246, 247, 248, 249, 250, 251};
    public static final int[] RS_HEX_10 = {264, 265, 266, 267, 268, 269};
    public static final int[] RS_HEX_11 = {270, 271, 272, 273, 274, 275};

    //Reed-Solomon(24,12,13) code protects the link control word.  Maximum correctable errors are:  6
    private static final ReedSolomon_24_12_13_P25 REED_SOLOMON_24_12_13_P25 = new ReedSolomon_24_12_13_P25();

    private LinkControlWord mLinkControlWord;

    public TDULCMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public P25P1DataUnitID getDUID()
    {
        return P25P1DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL;
    }

    public LinkControlWord getLinkControlWord()
    {
        if(mLinkControlWord == null)
        {
            createLinkControlWord();
        }

        return mLinkControlWord;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getLinkControlWord());

        return sb.toString();
    }

    /**
     * Checks and repairs the 12 x 24-bit golay(24,12,8) codewords and then
     * checks and repairs the 24 x 6-bit link control and reed-solomon
     * RS(24,12,13) parity codewords
     */
    private void createLinkControlWord()
    {
        //Check the Golay codewords
        int codewordPointer = 0;

        while(codewordPointer < getMessage().size())
        {
            int corrected = Golay24.checkAndCorrect(getMessage(), codewordPointer);
            codewordPointer += 24;
        }

        //Check the Reed-Solomon parity bits. The RS decoder expects the link control data and reed solomon parity hex
        // codewords in reverse order. The RS(24,12,13) code used by P25 removes the left-hand 47 data hex words, so we
        // replace them with zeros. */
        int[] input = new int[63];
        int[] output = new int[63];

        input[0] = getMessage().getInt(RS_HEX_11);
        input[1] = getMessage().getInt(RS_HEX_10);
        input[2] = getMessage().getInt(RS_HEX_9);
        input[3] = getMessage().getInt(RS_HEX_8);
        input[4] = getMessage().getInt(RS_HEX_7);
        input[5] = getMessage().getInt(RS_HEX_6);
        input[6] = getMessage().getInt(RS_HEX_5);
        input[7] = getMessage().getInt(RS_HEX_4);
        input[8] = getMessage().getInt(RS_HEX_3);
        input[9] = getMessage().getInt(RS_HEX_2);
        input[10] = getMessage().getInt(RS_HEX_1);
        input[11] = getMessage().getInt(RS_HEX_0);
        input[12] = getMessage().getInt(LC_HEX_11);
        input[13] = getMessage().getInt(LC_HEX_10);
        input[14] = getMessage().getInt(LC_HEX_9);
        input[15] = getMessage().getInt(LC_HEX_8);
        input[16] = getMessage().getInt(LC_HEX_7);
        input[17] = getMessage().getInt(LC_HEX_6);
        input[18] = getMessage().getInt(LC_HEX_5);
        input[19] = getMessage().getInt(LC_HEX_4);
        input[20] = getMessage().getInt(LC_HEX_3);
        input[21] = getMessage().getInt(LC_HEX_2);
        input[22] = getMessage().getInt(LC_HEX_1);
        input[23] = getMessage().getInt(LC_HEX_0);
        /* indexes 24 - 62 are defaulted to zero */

        boolean irrecoverableErrors = REED_SOLOMON_24_12_13_P25.decode(input, output);

        //Transfer error corrected output to a new binary message
        CorrectedBinaryMessage binaryMessage = new CorrectedBinaryMessage(72);

        int pointer = 0;

        for(int x = 23; x >= 12; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mLinkControlWord = LinkControlWordFactory.create(binaryMessage, getTimestamp(), true);
        mLinkControlWord.setValid(!irrecoverableErrors);

        //If we corrected any bit errors, update the original message with the bit error count
        for(int x = 0; x < 23; x++)
        {
            if(output[x] != input[x])
            {
                getMessage().incrementCorrectedBitCount(Integer.bitCount((output[x] ^ input[x])));
            }
        }
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        if(getLinkControlWord().isValid() && getLinkControlWord() instanceof IFrequencyBandReceiver)
        {
            return ((IFrequencyBandReceiver)getLinkControlWord()).getChannels();
        }

        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getLinkControlWord().getIdentifiers();
    }
}
