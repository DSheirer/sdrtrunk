/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.message.tdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.Golay24;
import io.github.dsheirer.edac.ReedSolomon_63_47_17;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.P25Message;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWordFactory;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.Collections;
import java.util.List;

public class TDULinkControlMessage extends P25Message implements FrequencyBandReceiver
{
    public static final int[] LC_HEX_0 = {64, 65, 66, 67, 68, 69};
    public static final int[] LC_HEX_1 = {70, 71, 72, 73, 74, 75};
    public static final int[] LC_HEX_2 = {88, 89, 90, 91, 92, 93};
    public static final int[] LC_HEX_3 = {94, 95, 96, 97, 98, 99};
    public static final int[] LC_HEX_4 = {112, 113, 114, 115, 116, 117};
    public static final int[] LC_HEX_5 = {118, 119, 120, 121, 122, 123};
    public static final int[] LC_HEX_6 = {136, 137, 138, 139, 140, 141};
    public static final int[] LC_HEX_7 = {142, 143, 144, 145, 146, 147};
    public static final int[] LC_HEX_8 = {160, 161, 162, 163, 164, 165};
    public static final int[] LC_HEX_9 = {166, 167, 168, 169, 170, 171};
    public static final int[] LC_HEX_10 = {184, 185, 186, 187, 188, 189};
    public static final int[] LC_HEX_11 = {190, 191, 192, 193, 194, 195};
    public static final int[] RS_HEX_0 = {208, 209, 210, 211, 212, 213};
    public static final int[] RS_HEX_1 = {214, 215, 216, 217, 218, 219};
    public static final int[] RS_HEX_2 = {232, 233, 234, 235, 236, 237};
    public static final int[] RS_HEX_3 = {238, 239, 240, 241, 242, 243};
    public static final int[] RS_HEX_4 = {256, 257, 258, 259, 260, 261};
    public static final int[] RS_HEX_5 = {262, 263, 264, 265, 266, 267};
    public static final int[] RS_HEX_6 = {280, 281, 282, 283, 284, 285};
    public static final int[] RS_HEX_7 = {286, 287, 288, 289, 290, 291};
    public static final int[] RS_HEX_8 = {304, 305, 306, 307, 308, 309};
    public static final int[] RS_HEX_9 = {310, 311, 312, 313, 314, 315};
    public static final int[] RS_HEX_10 = {328, 329, 330, 331, 332, 333};
    public static final int[] RS_HEX_11 = {334, 335, 336, 337, 338, 339};

    //Reed-Solomon(24,12,13) code protects the link control word.  Maximum correctable errors are: Hamming Distance(13) / 2 = 6
    public static final ReedSolomon_63_47_17 REED_SOLOMON_63_47_17 = new ReedSolomon_63_47_17(6);

    private LinkControlWord mLinkControlWord;

    public TDULinkControlMessage(CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(message, nac, timestamp);
    }

    @Override
    public DataUnitID getDUID()
    {
        return DataUnitID.TERMINATOR_DATA_UNIT_LINK_CONTROL;
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
        sb.append(" LINK CONTROL:").append(getLinkControlWord());

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

        boolean irrecoverableErrors = REED_SOLOMON_63_47_17.decode(input, output);

        //Transfer error corrected output to a new binary message
        BinaryMessage binaryMessage = new BinaryMessage(72);

        int pointer = 0;

        for(int x = 23; x >= 12; x--)
        {
            if(output[x] != -1)
            {
                binaryMessage.load(pointer, 6, output[x]);
            }

            pointer += 6;
        }

        mLinkControlWord = LinkControlWordFactory.create(binaryMessage);

        if(irrecoverableErrors)
        {
            mLinkControlWord.setValid(false);
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

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        if(getLinkControlWord().isValid() && getLinkControlWord() instanceof FrequencyBandReceiver)
        {
            return ((FrequencyBandReceiver)getLinkControlWord()).getChannels();
        }

        return Collections.EMPTY_LIST;
    }
}
