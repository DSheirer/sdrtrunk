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

package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import java.util.EnumSet;
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
    IDENTITY("22", -1, "IDENTITY"),
    UNKNOWN_23("23", 1, "UNKNOWN 23"),
    TRIGGER_PERIODIC("31", 1, "TRIGGER PERIODIC"),
    TIMESTAMP("34", 5, "TIMESTAMP"),
    VERSION("36", 1, "VERSION"),
    RESPONSE("37", -1, "RESPONSE"),
    SUCCESS( "38", 0, "SUCCESS"),
    REQUEST_3A("3A", 0,"REQUEST-3A"),
    TRIGGER_GPIO("42", 0, "TRIGGER GPIO"),
    TRIGGER_DISTANCE("4A", 1, "TRIGGER DISTANCE"), //Guess - not sure
    ALTITUDE_ACCURACY("50", 0,"ALTITUDE ACCURACY"),
    CIRCLE_2D("51", 10, "CIRCLE 2D"),
    TIME("52", 0,"TIME"),
    ALTITUDE("54", 0,"ALTITUDE"),
    CIRCLE_3D("55", 15, "CIRCLE 3D"),
    HEADING("56", 1, "HEADING"),
    HORIZONTAL_DIRECTION("57", 0,"HORIZONTAL DIRECTION"),
    REQUEST_61("61", 1, "REQUEST-61"),
    REQUEST_62("62", 0,"REQUEST-62"),
    REQUEST_64("64", 0,"REQUEST-64"),
    POINT_2D("66", 8, "POINT 2D"),
    POINT_3D("69", 11, "POINT 3D"),
    SPEED("6C", 2, "SPEED"),
    REQUEST_73("73", 1, "REQUEST-73"),
    TRIGGER_ON_MOVE("78", 1, "TRIGGER ON MOVE"),
    REQUESTED_TOKENS("000", 0, "REQUESTED TOKENS"),
    UNKNOWN("0",0, "UNKNOWN");

    private String mValue;
    private int mLength;
    private String mLabel;

    /**
     * Tokens that have parameters in request packet messages
     */
    private static EnumSet<TokenType> REQUEST_PARAMETER_TOKENS = EnumSet.of(IDENTITY, TRIGGER_DISTANCE, TRIGGER_GPIO,
            TRIGGER_ON_MOVE, TRIGGER_PERIODIC, REQUEST_61, REQUEST_73);

    /**
     * Constructs an instance
     * @param value of the token
     * @param length of the token, exclusive of the identifier, where -1 indicates a variable length token.
     */
    TokenType(String value, int length, String label)
    {
        mValue = value;
        mLength = length;
        mLabel = label;
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

    /**
     * Indicates if this is a request type token that has an optional parameter (e.g. the trigger tokens).
     */
    public boolean isRequestParameterToken()
    {
        return REQUEST_PARAMETER_TOKENS.contains(this);
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

    @Override
    public String toString()
    {
        return mLabel;
    }
}
