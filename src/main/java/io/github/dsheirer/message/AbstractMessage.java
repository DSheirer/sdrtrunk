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

package io.github.dsheirer.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.FragmentedIntField;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;

/**
 * Base message class with general utility methods for accessing fields in BinaryMessages.
 */
public abstract class AbstractMessage
{
    public static final int OCTET_0_BIT_0 = 0;
    public static final int OCTET_1_BIT_8 = 8;
    public static final int OCTET_2_BIT_16 = 16;
    public static final int OCTET_3_BIT_24 = 24;
    public static final int OCTET_4_BIT_32 = 32;
    public static final int OCTET_5_BIT_40 = 40;
    public static final int OCTET_6_BIT_48 = 48;
    public static final int OCTET_7_BIT_56 = 56;
    public static final int OCTET_8_BIT_64 = 64;
    public static final int OCTET_9_BIT_72 = 72;
    public static final int OCTET_10_BIT_80 = 80;
    public static final int OCTET_11_BIT_88 = 88;
    public static final int OCTET_12_BIT_96 = 96;

    private CorrectedBinaryMessage mMessage;
    private int mOffset = 0;

    /**
     * Constructs an instance
     * @param message containing the message bits.
     * @param offset to the start of the message within the corrected binary message.
     */
    public AbstractMessage(CorrectedBinaryMessage message, int offset)
    {
        mMessage = message;
        mOffset = offset;
    }

    /**
     * Constructs an instance.
     * @param message bits
     */
    public AbstractMessage(CorrectedBinaryMessage message)
    {
        this(message, 0);
    }

    /**
     * Indicates if this message is offset within the corrected binary message.
     * @return true of offset is non-zero.
     */
    private boolean hasOffset()
    {
        return mOffset > 0;
    }

    /**
     * Offset to the start of the message (fragement) within the corrected binary message.
     */
    public int getOffset()
    {
        return mOffset;
    }

    /**
     * Binary (bits) message for this message.
     * @return corrected binary message
     */
    public CorrectedBinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if the field has a non-zero value, meaning that any of the bits for the field are set to a one.
     * @param field to inspect.
     * @return true if the field has a non-zero value.
     */
    public boolean hasInt(IntField field)
    {
        if(hasOffset())
        {
            return getMessage().hasInt(field, getOffset());
        }
        else
        {
            return getMessage().hasInt(field);
        }
    }

    /**
     * Gets the integer field value and formats it as hexadecimal number
     * @param field to parse
     * @param places to format (ie 2 for a byte, 4 for 2-bytes, etc.)
     * @return formatted hex value.
     */
    public String getIntAsHex(IntField field, int places)
    {
        int value = getInt(field);
        String hex = Integer.toHexString(value).toUpperCase();
        while(hex.length() < places)
        {
            hex = "0" + hex;
        }

        return hex;
    }

    /**
     * Access the integer value for the specified field.
     * @param field to access
     * @return integer value for the field.
     */
    public int getInt(IntField field)
    {
        if(hasOffset())
        {
            return getMessage().getInt(field, getOffset());
        }
        else
        {
            return getMessage().getInt(field);
        }
    }

    /**
     * Access the integer value for the specified field at the specified offset.
     * @param field description
     * @param offset to the start of the field.
     * @return integer value.
     */
    public int getInt(IntField field, int offset)
    {
        return getMessage().getInt(field, offset);
    }

    /**
     * Indicates if the field has a non-zero value, meaning that any of the bits for the field are set to a one.
     * @param field to inspect.
     * @return true if the field has a non-zero value.
     */
    public boolean hasInt(FragmentedIntField field)
    {
        if(hasOffset())
        {
            return getMessage().hasInt(field, getOffset());
        }
        else
        {
            return getMessage().hasInt(field);
        }
    }

    /**
     * Access the integer value for the specified field.
     * @param field to access
     * @return integer value for the field.
     */
    public int getInt(FragmentedIntField field)
    {
        if(hasOffset())
        {
            return getMessage().getInt(field, getOffset());
        }
        else
        {
            return getMessage().getInt(field);
        }
    }

    /**
     * Access the long value for the specified field.
     * @param field to access
     * @return value for the field.
     */
    public long getLong(LongField field)
    {
        if(hasOffset())
        {
            return getMessage().getLong(field, getOffset());
        }
        else
        {
            return getMessage().getLong(field);
        }
    }

    /**
     * Utility method to format an octet as hex.
     */
    public static String formatOctetAsHex(int value)
    {
        return String.format("%02X", value);
    }
}
