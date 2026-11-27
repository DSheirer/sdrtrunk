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

package io.github.dsheirer.module.decode.pager.flex;

import io.github.dsheirer.module.decode.pager.PagerMessageType;
import io.github.dsheirer.module.decode.pager.PagerText;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses the 88 error corrected words of one FLEX frame phase into {@link FlexMessage}s.
 *
 * Each word carries 21 data bits, where bit 0 was received first.  The phase is organized as:
 * <ul>
 *     <li>Block information word (BIW) at word 0, which gives the start of the address field and the start of the
 *     vector field.</li>
 *     <li>Address field: one word per short address, or two words per long address.</li>
 *     <li>Vector field: one vector word per address word, which gives the message type and the location and length
 *     of the message words.  Short pages (tone only and short numeric) carry their content in the vector word.</li>
 *     <li>Message field: message content words.</li>
 * </ul>
 */
public class FlexMessageParser
{
    public static final int WORDS_PER_PHASE = 88;
    private static final int IDLE_WORD_ZEROS = 0x000000;
    private static final int IDLE_WORD_ONES = 0x1FFFFF;
    private static final int SHORT_ADDRESS_MIN = 0x008001;
    private static final int SHORT_ADDRESS_MAX = 0x1E0000;
    private static final int SHORT_ADDRESS_OFFSET = 0x8000;
    private static final long GROUP_CAPCODE_MIN = 2029568;
    private static final long GROUP_CAPCODE_MAX = 2029583;
    private static final String NUMERIC_CHARACTERS = "0123456789 U -][";
    private static final int NUMERIC_FILL = 0xC;
    private static final int ETX = 0x03;

    //Vector word message types
    private static final int TYPE_SECURE = 0;
    private static final int TYPE_SHORT_INSTRUCTION = 1;
    private static final int TYPE_TONE = 2;
    private static final int TYPE_STANDARD_NUMERIC = 3;
    private static final int TYPE_SPECIAL_NUMERIC = 4;
    private static final int TYPE_ALPHANUMERIC = 5;
    private static final int TYPE_BINARY = 6;
    private static final int TYPE_NUMBERED_NUMERIC = 7;

    private FlexMessageParser()
    {
    }

    /**
     * Parses the phase words.
     * @param words 88 data words (21 bits each) after error correction
     * @param valid flags for each word indicating that error correction succeeded
     * @param phase label (A-D)
     * @param mode of the frame
     * @param cycle from the frame information word
     * @param frame from the frame information word
     * @param timestamp of the frame
     * @return parsed messages, or an empty list
     */
    public static List<FlexMessage> parse(int[] words, boolean[] valid, char phase, FlexMode mode, int cycle,
                                          int frame, long timestamp)
    {
        List<FlexMessage> messages = new ArrayList<>();

        if(words.length < WORDS_PER_PHASE || valid.length < WORDS_PER_PHASE || !valid[0] || isIdle(words[0]))
        {
            return messages;
        }

        int biw = words[0];
        int addressStart = ((biw >>> 8) & 0x3) + 1;
        int vectorStart = (biw >>> 10) & 0x3F;

        if(vectorStart < addressStart || vectorStart >= WORDS_PER_PHASE)
        {
            return messages;
        }

        //Message words follow the vector field, which has one vector word per address word
        int messageStart = vectorStart + (vectorStart - addressStart);

        for(int address = addressStart; address < vectorStart; address++)
        {
            int vector = vectorStart + address - addressStart;

            if(vector >= WORDS_PER_PHASE)
            {
                break;
            }

            if(!valid[address] || isIdle(words[address]))
            {
                continue;
            }

            int addressWord = words[address];
            boolean longAddress = addressWord < SHORT_ADDRESS_MIN || addressWord > SHORT_ADDRESS_MAX;
            long capcode;

            if(longAddress)
            {
                //Long addresses use two address words and two vector words.
                if(address + 1 >= vectorStart || vector + 1 >= WORDS_PER_PHASE || !valid[address + 1])
                {
                    address++;
                    continue;
                }

                capcode = getLongCapcode(addressWord, words[address + 1]);
            }
            else
            {
                capcode = addressWord - SHORT_ADDRESS_OFFSET;
            }

            if(valid[vector])
            {
                FlexMessage message = parseVector(words, valid, vector, messageStart, capcode, longAddress, phase,
                    mode, cycle, frame, timestamp);

                if(message != null)
                {
                    messages.add(message);
                }
            }

            if(longAddress)
            {
                //Skip the second address word (and its vector word).
                address++;
            }
        }

        return messages;
    }

    /**
     * Calculates a long capcode from the two address words.
     *
     * Note: this formula follows the commonly used open source decoders and has not been verified against long
     * address transmissions.
     */
    static long getLongCapcode(int addressWord1, int addressWord2)
    {
        return addressWord1 + ((long)(addressWord2 ^ IDLE_WORD_ONES) << 15) + 0x1F9000L;
    }

    /**
     * Indicates if the word is an idle (all zeros or all ones) word.
     */
    private static boolean isIdle(int word)
    {
        return word == IDLE_WORD_ZEROS || word == IDLE_WORD_ONES;
    }

    /**
     * Parses the message referenced by the vector word.
     */
    private static FlexMessage parseVector(int[] words, boolean[] valid, int vector, int messageStart, long capcode,
                                           boolean longAddress, char phase, FlexMode mode, int cycle, int frame,
                                           long timestamp)
    {
        int vectorWord = words[vector];
        int type = (vectorWord >>> 4) & 0x7;
        boolean group = capcode >= GROUP_CAPCODE_MIN && capcode <= GROUP_CAPCODE_MAX;
        PagerMessageType messageType;
        String content;
        int[] errors = new int[1];
        FlexMessage.Fragment fragment = FlexMessage.Fragment.NONE;

        switch(type)
        {
            case TYPE_SECURE:
            case TYPE_ALPHANUMERIC:
                messageType = type == TYPE_SECURE ? PagerMessageType.SECURE : PagerMessageType.ALPHANUMERIC;
                int start = (vectorWord >>> 7) & 0x7F;
                int length = (vectorWord >>> 14) & 0x7F;
                int end = start + length - 1;

                if(start < messageStart || length < 1 || end >= WORDS_PER_PHASE)
                {
                    return null;
                }

                if(!valid[start])
                {
                    errors[0]++;
                }

                int header = words[start];
                int fragmentNumber = (header >>> 11) & 0x3;
                boolean continued = ((header >>> 10) & 0x1) == 1;

                if(continued)
                {
                    fragment = FlexMessage.Fragment.MORE_FOLLOWS;
                }
                else if(fragmentNumber == 3)
                {
                    fragment = FlexMessage.Fragment.COMPLETE;
                }
                else
                {
                    fragment = FlexMessage.Fragment.LAST;
                }

                content = parseAlphanumeric(words, valid, start + 1, end, fragmentNumber == 3, errors);
                break;
            case TYPE_SHORT_INSTRUCTION:
                messageType = PagerMessageType.SHORT_INSTRUCTION;
                int assignedFrame = (vectorWord >>> 10) & 0x7F;
                int groupBit = (vectorWord >>> 17) & 0x0F;
                content = "GROUP ASSIGNMENT BIT:" + groupBit + " FRAME:" + assignedFrame;
                break;
            case TYPE_TONE:
                if(((vectorWord >>> 7) & 0x3) == 0)
                {
                    messageType = PagerMessageType.NUMERIC;
                    content = parseShortNumeric(words, valid, vector, longAddress, errors);
                }
                else
                {
                    messageType = PagerMessageType.TONE;
                    content = "";
                }
                break;
            case TYPE_STANDARD_NUMERIC:
            case TYPE_SPECIAL_NUMERIC:
            case TYPE_NUMBERED_NUMERIC:
                messageType = PagerMessageType.NUMERIC;
                if(((vectorWord >>> 7) & 0x7F) < messageStart)
                {
                    return null;
                }

                content = parseNumeric(words, valid, vector, longAddress, type == TYPE_NUMBERED_NUMERIC, errors);

                if(content == null)
                {
                    return null;
                }
                break;
            case TYPE_BINARY:
            default:
                messageType = PagerMessageType.BINARY;
                int binaryStart = (vectorWord >>> 7) & 0x7F;
                int binaryLength = (vectorWord >>> 14) & 0x7F;
                int binaryEnd = binaryStart + binaryLength - 1;

                if(binaryStart < messageStart || binaryLength < 1 || binaryEnd >= WORDS_PER_PHASE)
                {
                    return null;
                }

                content = parseBinary(words, valid, binaryStart, binaryEnd, errors);
                break;
        }

        //Reject pages where most of the message words are uncorrectable - the vector word is likely a false decode.
        int messageWordCount = getMessageWordCount(vectorWord, type, longAddress);

        if(messageWordCount > 0 && errors[0] * 2 > messageWordCount)
        {
            return null;
        }

        return new FlexMessage(timestamp, capcode, messageType, content, mode, cycle, frame, phase, longAddress,
            group, fragment, errors[0]);
    }

    /**
     * Number of message words referenced by the vector word, or 0 for pages carried in the vector word.
     */
    private static int getMessageWordCount(int vectorWord, int type, boolean longAddress)
    {
        return switch(type)
        {
            case TYPE_SECURE, TYPE_ALPHANUMERIC, TYPE_BINARY -> (vectorWord >>> 14) & 0x7F;
            case TYPE_STANDARD_NUMERIC, TYPE_SPECIAL_NUMERIC, TYPE_NUMBERED_NUMERIC -> ((vectorWord >>> 14) & 0x7) + 1;
            default -> longAddress ? 1 : 0;
        };
    }

    /**
     * Parses alphanumeric content: three 7-bit characters per word in bits 0-6, 7-13 and 14-20.
     * @param first data word (after the fragment header word)
     * @param last data word (inclusive)
     * @param skipFirstCharacter true if the first character of the first word is a message signature, not content
     * @param errors counter
     */
    static String parseAlphanumeric(int[] words, boolean[] valid, int first, int last, boolean skipFirstCharacter,
                                    int[] errors)
    {
        StringBuilder sb = new StringBuilder();

        for(int index = first; index <= last; index++)
        {
            if(!valid[index])
            {
                errors[0]++;
                sb.append(PagerText.ERROR_CHARACTER).append(PagerText.ERROR_CHARACTER).append(PagerText.ERROR_CHARACTER);
                continue;
            }

            int word = words[index];

            for(int shift = 0; shift <= 14; shift += 7)
            {
                if(index == first && shift == 0 && skipFirstCharacter)
                {
                    continue;
                }

                int character = (word >>> shift) & 0x7F;

                if(character != ETX)
                {
                    PagerText.appendAscii(sb, character);
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Parses standard, special and numbered numeric content: 4-bit characters, LSB first, packed continuously across
     * the 21 data bits of each message word.  Standard and special numeric pages start with 2 signature bits.
     * Numbered numeric pages start with 12 bits of signature and message number.
     *
     * For a short address, the message words are the start word through start + length, where start (bits 7-13) and
     * length (bits 14-16) are from the vector word.  For a long address, the second vector word is the first message
     * word.
     *
     * @return content or null if the vector word references words outside of the phase.
     */
    static String parseNumeric(int[] words, boolean[] valid, int vector, boolean longAddress, boolean numbered,
                               int[] errors)
    {
        int vectorWord = words[vector];
        int start = (vectorWord >>> 7) & 0x7F;
        int length = (vectorWord >>> 14) & 0x7;

        List<Integer> messageWords = new ArrayList<>();

        if(longAddress)
        {
            messageWords.add(vector + 1);

            for(int index = start; index < start + length; index++)
            {
                messageWords.add(index);
            }
        }
        else
        {
            for(int index = start; index <= start + length; index++)
            {
                messageWords.add(index);
            }
        }

        for(int index: messageWords)
        {
            if(index < 1 || index >= WORDS_PER_PHASE)
            {
                return null;
            }
        }

        StringBuilder sb = new StringBuilder();
        int skip = numbered ? 12 : 2;
        int digit = 0;
        int bitCount = 0;
        boolean digitError = false;

        for(int index: messageWords)
        {
            boolean wordValid = valid[index];

            if(!wordValid)
            {
                errors[0]++;
            }

            int word = words[index];

            for(int bit = 0; bit < 21; bit++)
            {
                if(skip > 0)
                {
                    skip--;
                    continue;
                }

                if(!wordValid)
                {
                    digitError = true;
                }
                else if(((word >>> bit) & 1) == 1)
                {
                    digit |= 1 << bitCount;
                }

                if(++bitCount == 4)
                {
                    if(digitError)
                    {
                        sb.append(PagerText.ERROR_CHARACTER);
                    }
                    else if(digit != NUMERIC_FILL)
                    {
                        sb.append(NUMERIC_CHARACTERS.charAt(digit));
                    }

                    digit = 0;
                    bitCount = 0;
                    digitError = false;
                }
            }
        }

        return sb.toString().trim();
    }

    /**
     * Parses a short numeric page carried in the vector word: three digits in bits 9-20, plus five digits in bits
     * 0-19 of the second vector word for a long address.
     */
    static String parseShortNumeric(int[] words, boolean[] valid, int vector, boolean longAddress, int[] errors)
    {
        StringBuilder sb = new StringBuilder();
        int vectorWord = words[vector];

        for(int shift = 9; shift <= 17; shift += 4)
        {
            appendDigit(sb, (vectorWord >>> shift) & 0xF);
        }

        if(longAddress)
        {
            if(valid[vector + 1])
            {
                int second = words[vector + 1];

                for(int shift = 0; shift <= 16; shift += 4)
                {
                    appendDigit(sb, (second >>> shift) & 0xF);
                }
            }
            else
            {
                errors[0]++;
                sb.append("?????");
            }
        }

        return sb.toString().trim();
    }

    private static void appendDigit(StringBuilder sb, int digit)
    {
        if(digit != NUMERIC_FILL)
        {
            sb.append(NUMERIC_CHARACTERS.charAt(digit));
        }
    }

    /**
     * Formats binary content as hexadecimal words.
     */
    static String parseBinary(int[] words, boolean[] valid, int first, int last, int[] errors)
    {
        StringBuilder sb = new StringBuilder();

        for(int index = first; index <= last; index++)
        {
            if(sb.length() > 0)
            {
                sb.append(' ');
            }

            if(valid[index])
            {
                sb.append(String.format("%06X", words[index]));
            }
            else
            {
                errors[0]++;
                sb.append("??????");
            }
        }

        return sb.toString();
    }
}
