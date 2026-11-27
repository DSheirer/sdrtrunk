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

package io.github.dsheirer.module.decode.pager;

/**
 * Text utilities shared by the paging message parsers.
 */
public final class PagerText
{
    /**
     * Character used in place of characters from codewords that could not be corrected.
     */
    public static final char ERROR_CHARACTER = '?';

    private PagerText()
    {
    }

    /**
     * Appends a 7-bit ASCII character to the message text.  Line feeds, carriage returns and tabs are shown as a
     * space so that the message is displayed on a single line, and the remaining control characters (including the
     * NUL, ETX and EOT fill characters) are dropped.
     *
     * @param sb to append to
     * @param character 7-bit ASCII value
     */
    public static void appendAscii(StringBuilder sb, int character)
    {
        character &= 0x7F;

        if(character == '\n' || character == '\r' || character == '\t')
        {
            //Collapse consecutive line breaks (e.g. CR LF) into a single space
            if(sb.length() > 0 && sb.charAt(sb.length() - 1) != ' ')
            {
                sb.append(' ');
            }
        }
        else if(character >= 0x20 && character < 0x7F)
        {
            sb.append((char)character);
        }
    }

    /**
     * Ratio of characters in the text that are digits or numeric page punctuation.
     */
    public static double getNumericRatio(String text)
    {
        if(text.isEmpty())
        {
            return 0.0;
        }

        int count = 0;

        for(char c: text.toCharArray())
        {
            if(Character.isDigit(c) || c == ' ' || c == '-')
            {
                count++;
            }
        }

        return count / (double)text.length();
    }

    /**
     * Ratio of characters in the text that are letters, digits or spaces.
     */
    public static double getWordCharacterRatio(String text)
    {
        if(text.isEmpty())
        {
            return 0.0;
        }

        int count = 0;

        for(char c: text.toCharArray())
        {
            if(Character.isLetterOrDigit(c) || c == ' ')
            {
                count++;
            }
        }

        return count / (double)text.length();
    }

    /**
     * Ratio of characters in the text that are letters, digits, white space or common punctuation.
     */
    public static double getTextRatio(String text)
    {
        if(text.isEmpty())
        {
            return 0.0;
        }

        int count = 0;

        for(char c: text.toCharArray())
        {
            if(Character.isLetterOrDigit(c) || c == ' ' || ".,:;-/()#@!?'\"&*+=%$<>_".indexOf(c) >= 0)
            {
                count++;
            }
        }

        return count / (double)text.length();
    }
}
