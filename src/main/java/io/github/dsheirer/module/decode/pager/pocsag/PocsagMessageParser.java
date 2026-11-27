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

package io.github.dsheirer.module.decode.pager.pocsag;

import io.github.dsheirer.module.decode.pager.PagerMessageType;
import io.github.dsheirer.module.decode.pager.PagerText;
import java.util.List;

/**
 * Parses the address and message codewords of a POCSAG page into a {@link PocsagMessage}.
 *
 * Each message codeword carries 20 data bits, transmitted MSB first.
 *
 * Numeric content: each codeword carries five 4-bit BCD characters.  Each character is transmitted LSB first.
 *
 * Alphanumeric content: the data bits of all message codewords form a continuous bit stream that carries 7-bit ASCII
 * characters.  Each character is transmitted LSB first, and characters may span codeword boundaries.
 *
 * The content type is signalled by the function code by convention only: function 0 is normally numeric and function
 * 3 is normally alphanumeric.  Since paging systems vary, the content is also tested to choose the more plausible
 * type when the function code and the content disagree.
 */
public class PocsagMessageParser
{
    /**
     * Marker for a message codeword that could not be corrected.
     */
    public static final int ERROR_WORD = -1;

    private static final String NUMERIC_CHARACTERS = "0123456789*U -][";
    private static final double PLAUSIBLE_RATIO = 0.9;
    private static final double PLAUSIBLE_WORD_CHARACTER_RATIO = 0.75;

    private PocsagMessageParser()
    {
    }

    /**
     * Parses the page.
     * @param timestamp of the address codeword
     * @param capcode 21-bit address (18 address bits from the address codeword plus the 3-bit frame number)
     * @param function code from the address codeword
     * @param messageWords 20-bit data values of the message codewords, in order, with {@link #ERROR_WORD} for
     * codewords that could not be corrected.
     * @param bitRate of the transmission
     * @param afsk true if the page was sent as AFSK audio tones
     * @return parsed message
     */
    public static PocsagMessage parse(long timestamp, int capcode, int function, List<Integer> messageWords,
                                      int bitRate, boolean afsk)
    {
        int errors = 0;

        for(Integer word: messageWords)
        {
            if(word == ERROR_WORD)
            {
                errors++;
            }
        }

        if(messageWords.isEmpty())
        {
            return new PocsagMessage(timestamp, capcode, function, PagerMessageType.TONE, "", bitRate, 0, afsk);
        }

        String numeric = parseNumeric(messageWords);
        String alpha = parseAlphanumeric(messageWords);

        PagerMessageType type = getMessageType(function, numeric, alpha);
        String content = switch(type)
        {
            case ALPHANUMERIC -> alpha;
            case NUMERIC -> numeric;
            default -> parseBinary(messageWords);
        };
        return new PocsagMessage(timestamp, capcode, function, type, content, bitRate, errors, afsk);
    }

    /**
     * Determines the content type from the function code and the plausibility of each decoding.  Content that is
     * neither plausible numeric nor plausible text (e.g. encrypted or system specific content) is shown as binary,
     * except for function 3 which is shown as alphanumeric.
     */
    static PagerMessageType getMessageType(int function, String numeric, String alpha)
    {
        boolean numericPlausible = PagerText.getNumericRatio(numeric) >= PLAUSIBLE_RATIO;
        boolean alphaPlausible = alpha.length() > 0 && PagerText.getTextRatio(alpha) >= PLAUSIBLE_RATIO &&
            PagerText.getWordCharacterRatio(alpha) >= PLAUSIBLE_WORD_CHARACTER_RATIO;

        if(function == 3)
        {
            return (alphaPlausible || !numericPlausible) ? PagerMessageType.ALPHANUMERIC : PagerMessageType.NUMERIC;
        }

        //Function 0 is normally numeric.  Functions 1 and 2 are system defined, so prefer plausible text.
        if(function == 0 && numericPlausible)
        {
            return PagerMessageType.NUMERIC;
        }

        if(alphaPlausible && !numericPlausible)
        {
            return PagerMessageType.ALPHANUMERIC;
        }

        return numericPlausible ? PagerMessageType.NUMERIC : PagerMessageType.BINARY;
    }

    /**
     * Formats the message codewords as hexadecimal 20-bit values.
     */
    static String parseBinary(List<Integer> messageWords)
    {
        StringBuilder sb = new StringBuilder();

        for(Integer word: messageWords)
        {
            if(sb.length() > 0)
            {
                sb.append(' ');
            }

            sb.append(word == ERROR_WORD ? "?????" : String.format("%05X", word));
        }

        return sb.toString();
    }

    /**
     * Decodes numeric (BCD) content.
     */
    static String parseNumeric(List<Integer> messageWords)
    {
        StringBuilder sb = new StringBuilder();

        for(Integer word: messageWords)
        {
            for(int digit = 0; digit < 5; digit++)
            {
                if(word == ERROR_WORD)
                {
                    sb.append(PagerText.ERROR_CHARACTER);
                }
                else
                {
                    int value = (word >>> (16 - (digit * 4))) & 0xF;
                    sb.append(NUMERIC_CHARACTERS.charAt(Integer.reverse(value) >>> 28));
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Decodes alphanumeric (7-bit ASCII) content.
     */
    static String parseAlphanumeric(List<Integer> messageWords)
    {
        StringBuilder sb = new StringBuilder();
        int character = 0;
        int bitCount = 0;
        boolean characterError = false;

        for(Integer word: messageWords)
        {
            for(int bit = 19; bit >= 0; bit--)
            {
                if(word == ERROR_WORD)
                {
                    characterError = true;
                }
                else if(((word >>> bit) & 1) == 1)
                {
                    character |= 1 << bitCount;
                }

                bitCount++;

                if(bitCount == 7)
                {
                    if(characterError)
                    {
                        sb.append(PagerText.ERROR_CHARACTER);
                    }
                    else
                    {
                        PagerText.appendAscii(sb, character);
                    }

                    character = 0;
                    bitCount = 0;
                    characterError = false;
                }
            }
        }

        return sb.toString().trim();
    }
}
