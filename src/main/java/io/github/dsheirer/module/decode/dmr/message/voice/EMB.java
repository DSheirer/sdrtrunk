/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * DMR Voice Frame B-F Embedded Signalling Chunk
 */
public class EMB
{
    private final static Logger mLog = LoggerFactory.getLogger(EMB.class);
    private static final int[] VALID_WORDS = new int[]{
        0x0000, 0x0273, 0x04E5, 0x0696, 0x09C9, 0x0BBA, 0x0D2C, 0x0F5F, 0x11E2, 0x1391, 0x1507, 0x1774, 0x182B, 0x1A58,
        0x1CCE, 0x1EBD, 0x21B7, 0x23C4, 0x2552, 0x2721, 0x287E, 0x2A0D, 0x2C9B, 0x2EE8, 0x3055, 0x3226, 0x34B0, 0x36C3,
        0x399C, 0x3BEF, 0x3D79, 0x3F0A, 0x411E, 0x436D, 0x45FB, 0x4788, 0x48D7, 0x4AA4, 0x4C32, 0x4E41, 0x50FC, 0x528F,
        0x5419, 0x566A, 0x5935, 0x5B46, 0x5DD0, 0x5FA3, 0x60A9, 0x62DA, 0x644C, 0x663F, 0x6960, 0x6B13, 0x6D85, 0x6FF6,
        0x714B, 0x7338, 0x75AE, 0x77DD, 0x7882, 0x7AF1, 0x7C67, 0x7E14, 0x802F, 0x825C, 0x84CA, 0x86B9, 0x89E6, 0x8B95,
        0x8D03, 0x8F70, 0x91CD, 0x93BE, 0x9528, 0x975B, 0x9804, 0x9A77, 0x9CE1, 0x9E92, 0xA198, 0xA3EB, 0xA57D, 0xA70E,
        0xA851, 0xAA22, 0xACB4, 0xAEC7, 0xB07A, 0xB209, 0xB49F, 0xB6EC, 0xB9B3, 0xBBC0, 0xBD56, 0xBF25, 0xC131, 0xC342,
        0xC5D4, 0xC7A7, 0xC8F8, 0xCA8B, 0xCC1D, 0xCE6E, 0xD0D3, 0xD2A0, 0xD436, 0xD645, 0xD91A, 0xDB69, 0xDDFF, 0xDF8C,
        0xE086, 0xE2F5, 0xE463, 0xE610, 0xE94F, 0xEB3C, 0xEDAA, 0xEFD9, 0xF164, 0xF317, 0xF581, 0xF7F2, 0xF8AD, 0xFADE,
        0xFC48, 0xFE3B};
    private static final HashSet<Integer> WORD_SET = new HashSet<>();

    static
    {
        for(int word: VALID_WORDS)
        {
            WORD_SET.add(word);
        }
    }

    private static final int[] WORD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] COLOR_CODE = new int[]{0, 1, 2, 3};
    private static final int ENCRYPTION_PI = 4;
    private static final int[] LINK_CONTROL_START_STOP = new int[]{5, 6};

    private CorrectedBinaryMessage mMessage;
    private boolean mValid = true;

    /**
     * Constructs an instance
     *
     * @param message with transmitted bits
     */
    public EMB(CorrectedBinaryMessage message)
    {
        mMessage = message;
        checkCRC();
    }

    /**
     * Checks the EMB word using the Quadratic Residue(16,7,6) code as detailed in TS 102 361-1 B.3.2.
     *
     * Implementation Notes:
     * There are 7 information bits protected by 9 parity bits with a Hamming distance of 6 meaning that this code can
     * correct up to 3 bit errors.  The VALID_WORDS constant is a list of all possible (128) valid words precalculated
     * with their corresponding QR checksum values.  When performing a CRC check, if the complete 16-bit value matches
     * any of these valid words, then the value has no errors.  Otherwise, XOR the received value against all valid
     * words and select any valid word that differs from the received word by no more than 3 bit position differences.
     */
    private void checkCRC()
    {
        int word = getMessage().getInt(WORD);

        if(WORD_SET.contains(word))
        {
            mValid = true;
            return;
        }

        int bitErrors = 0;

        for(int validWord: VALID_WORDS)
        {
            bitErrors = Integer.bitCount(word ^ validWord);

            if(bitErrors <= 3)
            {
                getMessage().setInt(validWord, WORD);
                getMessage().setCorrectedBitCount(bitErrors);
                mValid = bitErrors <= 1;  //Only accept up to 1 bit error for validity
                return;
            }
        }

        mValid = false;
    }

    /**
     * Indicates if this message is valid and has passed CRC check
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Message bits
     */
    protected CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Color code
     */
    public int getColorCode()
    {
        return getMessage().getInt(COLOR_CODE);
    }

    /**
     * Link Control Start-Stop
     */
    public LCSS getLCSS()
    {
        return LCSS.fromValue(getMessage().getInt(LINK_CONTROL_START_STOP));
    }

    /**
     * Indicates if this call is encrypted
     */
    public boolean isEncrypted()
    {
        return getMessage().get(ENCRYPTION_PI);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" EMB-");
        sb.append(isEncrypted() ? "EN" : "UN");
        sb.append(getLCSS());
        sb.append("CC:").append(getColorCode());
        return sb.toString();
    }

    public static void main(String[] args)
    {
        CorrectedBinaryMessage m = new CorrectedBinaryMessage(16);

        for(int x = 0; x < Math.pow(2, 16); x++)
        {
            m.setInt(x, WORD);

            EMB emb = new EMB(m);

            if(!emb.isValid())
            {
                mLog.debug("Invalid: " + x);
            }
        }


    }
}
