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

package io.github.dsheirer.module.decode.ip.lrrp.token;

import java.util.HashMap;
import java.util.Map;

/**
 * LRRP Token Types
 *
 * Variable length tokens consist of an identifier, a length indicator, and then a sequence of bytes equal in length
 * to the length indicator.
 */
public enum TokenType
{
    POSITION("66", 8),
    POSITION_3D("51", 10),
    HEADING("56", 1),
    SPEED("6C", 2),
    TIMESTAMP("34", 5),
    IDENTITY("22", -1),
    UNKNOWN_23("23", 1),
    UNKNOWN("0",0);

    private String mValue;
    private int mLength;

    /**
     * Constructs an instance
     * @param value of the token
     * @param length of the token, exclusive of the identifier, where -1 indicates a variable length token.
     */
    TokenType(String value, int length)
    {
        mValue = value;
        mLength = length;
    }

    /**
     * String (Numeric) value of the token
     */
    public String getValue()
    {
        return mValue;
    }

    /**
     * Indicates if this token is variable length.
     */
    public boolean isVariableLength()
    {
        return mLength == -1;
    }

    /**
     * Length of the token value in bytes.
     * Note: byte count does not include the token byte itself.
     */
    public int getLength()
    {
        return mLength;
    }

    private static final Map<String,TokenType> LOOKUP_MAP = new HashMap<>();

    static
    {
        for(TokenType tokenType: TokenType.values())
        {
            LOOKUP_MAP.put(tokenType.getValue(), tokenType);
        }
    }

    /**
     * Lookup a token from a 2-character string value
     * @param value that represents the token
     * @return matching token or UNKNOWN.
     */
    public static TokenType fromValue(String value)
    {
        if(value != null && LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }
}
