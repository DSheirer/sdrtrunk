/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

/**
 * Payload Formats for Defined Short Data packets
 */
public enum DefinedDataFormat
{
    BINARY,
    BCD,
    ASCII_7,
    ISO_IEC_8859_1,
    ISO_IEC_8859_2,
    ISO_IEC_8859_3,
    ISO_IEC_8859_4,
    ISO_IEC_8859_5,
    ISO_IEC_8859_6,
    ISO_IEC_8859_7,
    ISO_IEC_8859_8,
    ISO_IEC_8859_9,
    ISO_IEC_8859_10,
    ISO_IEC_8859_11,
    ISO_IEC_8859_12,
    ISO_IEC_8859_13,
    ISO_IEC_8859_14,
    ISO_IEC_8859_15,
    ISO_IEC_8859_16,
    UNICODE_UTF_8,
    UNICODE_UTF_16,
    UNICODE_UTF_16_BE,
    UNICODE_UTF_16_LE,
    UNICODE_UTF_32,
    UNICODE_UTF_32_BE,
    UNICODE_UTF_32_LE,
    UNKNOWN;

    public static DefinedDataFormat fromValue(int value)
    {
        switch(value)
        {
            case 0:
                return BINARY;
            case 1:
                return BCD;
            case 2:
                return ASCII_7;
            case 3:
                return ISO_IEC_8859_1;
            case 4:
                return ISO_IEC_8859_2;
            case 5:
                return ISO_IEC_8859_3;
            case 6:
                return ISO_IEC_8859_4;
            case 7:
                return ISO_IEC_8859_5;
            case 8:
                return ISO_IEC_8859_6;
            case 9:
                return ISO_IEC_8859_7;
            case 10:
                return ISO_IEC_8859_8;
            case 11:
                return ISO_IEC_8859_9;
            case 12:
                return ISO_IEC_8859_10;
            case 13:
                return ISO_IEC_8859_11;
            case 14:
                return ISO_IEC_8859_13;
            case 15:
                return ISO_IEC_8859_14;
            case 16:
                return ISO_IEC_8859_15;
            case 17:
                return ISO_IEC_8859_16;
            case 18:
                return UNICODE_UTF_8;
            case 19:
                return UNICODE_UTF_16;
            case 20:
                return UNICODE_UTF_16_BE;
            case 21:
                return UNICODE_UTF_16_LE;
            case 22:
                return UNICODE_UTF_32;
            case 23:
                return UNICODE_UTF_32_BE;
            case 24:
                return UNICODE_UTF_32_LE;
            default:
                return UNKNOWN;
        }
    }
}
