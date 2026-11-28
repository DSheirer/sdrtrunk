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

package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.LCSS;
import java.util.HashSet;

/**
 * DMR Voice Frame B-F Embedded Signalling Chunk
 */
public class EMB
{
    //Quadratic Residue (17,9) generator polynomial g(x) = x^8 + x^5 + x^4 + x^3 + 1, shortened to (16,7) after
    //appending an even parity bit.
    private static final int GENERATOR = 0x139;
    private static final int INFORMATION_WORD_COUNT = 128;
    private static final int[] VALID_WORDS = new int[INFORMATION_WORD_COUNT];
    private static final HashSet<Integer> WORD_SET = new HashSet<>();

    static
    {
        for(int information = 0; information < INFORMATION_WORD_COUNT; information++)
        {
            VALID_WORDS[information] = encode(information);
            WORD_SET.add(VALID_WORDS[information]);
        }
    }

    private static final int[] WORD = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] COLOR_CODE = new int[]{0, 1, 2, 3};
    private static final int PREEMPTION_POWER_CONTROL_INDICATOR = 4;
    private static final int[] LINK_CONTROL_START_STOP = new int[]{5, 6};
    //Maximum correctable bit errors for a minimum Hamming distance 6 code: (6 - 1) / 2 = 2.  Any decode that differs
    //from the received word by more than this is ambiguous and is flagged invalid by both the hard and soft decoders.
    private static final int MAX_CORRECTABLE_BIT_ERRORS = 2;

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
     * Constructs an instance using maximum likelihood soft decision decoding when the soft bits are provided.
     *
     * @param message with transmitted bits
     * @param softBits for the 16 transmitted bits where a positive value favors a 1 bit, or null.
     */
    public EMB(CorrectedBinaryMessage message, float[] softBits)
    {
        mMessage = message;

        if(softBits != null && softBits.length == WORD.length)
        {
            decodeSoft(softBits);
        }
        else
        {
            checkCRC();
        }
    }

    /**
     * Encodes the 7-bit information word (color code, privacy indicator and LCSS) into the 16-bit QR(16,7,6)
     * codeword: 7 information bits, followed by the 8 bit remainder of the information word times x^8 divided by the
     * generator polynomial, followed by an even parity bit over the preceding 15 bits.
     * @param information word in the range 0-127
     * @return codeword
     */
    static int encode(int information)
    {
        int remainder = information << 8;

        for(int bit = 14; bit >= 8; bit--)
        {
            if((remainder & (1 << bit)) != 0)
            {
                remainder ^= GENERATOR << (bit - 8);
            }
        }

        int word = (information << 8) | remainder;
        return (word << 1) | (Integer.bitCount(word) & 1);
    }

    /**
     * Copy of the 128 valid codewords, indexed by information word.
     */
    static int[] getValidWords()
    {
        return VALID_WORDS.clone();
    }

    /**
     * Maximum likelihood soft decision decoding against all valid words.  Selects the valid word with the lowest sum
     * of soft bit magnitudes for the bit positions where the valid word differs from the hard decision.  The message
     * is always set to the selected word, but it is only flagged valid when the selected word is within the
     * correctable bit error count.  Beyond that count, the selected word is still the best estimate available and is
     * a better source for the LCSS used to assemble the embedded full link control fragments than the received bits.
     * @param softBits for the 16 transmitted bits.
     */
    private void decodeSoft(float[] softBits)
    {
        int word = getMessage().getInt(WORD);
        int best = word;
        float bestMetric = Float.MAX_VALUE;

        for(int validWord: VALID_WORDS)
        {
            int difference = word ^ validWord;
            float metric = 0.0f;

            while(difference != 0)
            {
                int bit = Integer.numberOfTrailingZeros(difference);
                //Word bit 15 is message bit 0
                metric += Math.abs(softBits[15 - bit]);
                difference &= difference - 1;
            }

            if(metric < bestMetric)
            {
                bestMetric = metric;
                best = validWord;
            }
        }

        int bitErrors = Integer.bitCount(word ^ best);

        if(bitErrors > 0)
        {
            getMessage().setInt(best, WORD);
            getMessage().setCorrectedBitCount(bitErrors);
        }

        mValid = bitErrors <= MAX_CORRECTABLE_BIT_ERRORS;
    }

    /**
     * Checks the EMB word using the Quadratic Residue(16,7,6) code as detailed in TS 102 361-1 B.3.2.
     *
     * Implementation Notes:
     * There are 7 information bits protected by 9 parity bits with a minimum Hamming distance of 6 meaning that this
     * code can correct up to 2 bit errors and detect 3 bit errors.  The VALID_WORDS constant is a list of all
     * possible (128) valid words.  If the complete 16-bit value matches any of these valid words, then the value has
     * no errors.  Otherwise, select the valid word that differs from the received word by no more than 2 bit
     * positions.  The distance 6 guarantees that there is at most one such word.  When there is no such word, the
     * message is left unchanged and flagged invalid.  Unlike the soft decoder, there is no unique best estimate at 3
     * or more bit errors, and the received bits are as likely to hold the correct LCSS as the nearest valid word.
     */
    private void checkCRC()
    {
        int word = getMessage().getInt(WORD);

        if(WORD_SET.contains(word))
        {
            mValid = true;
            return;
        }

        for(int validWord: VALID_WORDS)
        {
            int bitErrors = Integer.bitCount(word ^ validWord);

            if(bitErrors <= MAX_CORRECTABLE_BIT_ERRORS)
            {
                getMessage().setInt(validWord, WORD);
                getMessage().setCorrectedBitCount(bitErrors);
                mValid = true;
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
     * Number of bits corrected when decoding the EMB word.
     */
    public int getCorrectedBitCount()
    {
        return getMessage().getCorrectedBitCount();
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
     * Pre-emption and power control indicator (PI), TS 102 361-1 9.3.2.  When set, the embedded signalling in this burst
     * carries reverse channel information for the other logical channel; when clear, it carries this channel's embedded
     * signalling or the null embedded message.  It does not indicate encryption.
     */
    public boolean isReverseChannel()
    {
        return getMessage().get(PREEMPTION_POWER_CONTROL_INDICATOR);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(" EMB-");
        sb.append(isReverseChannel() ? "RC-" : "");
        sb.append(getLCSS());
        sb.append("CC:").append(getColorCode());
        return sb.toString();
    }
}
