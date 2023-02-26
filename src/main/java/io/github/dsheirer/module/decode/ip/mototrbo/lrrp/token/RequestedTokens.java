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

import com.google.common.base.Joiner;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * LRRP Requested Element Token
 *
 * Start Token: various
 * Total Length: 1 byte
 */
public class RequestedTokens extends Token
{
    private List<TokenType> mRequestedTokens = new ArrayList<>();
    private List<String> mUnrecognizedRequestedTokens = new ArrayList<>();

    /**
     * Constructs an instance of a heading token.
     */
    public RequestedTokens()
    {
        super(null, 0);
    }

    @Override
    public TokenType getTokenType()
    {
    return TokenType.REQUESTED_TOKENS;
    }

    /**
     * Adds the recognized requested token type
     */
    public void add(TokenType tokenType)
    {
        mRequestedTokens.add(tokenType);
    }

    public void add(String unrecognizedTokenType)
    {
        mUnrecognizedRequestedTokens.add(unrecognizedTokenType);
        Collections.sort(mUnrecognizedRequestedTokens);
    }

    /**
     * Indicates if this object has requested tokens.
     */
    public boolean hasRequestedTokens()
    {
        return !mRequestedTokens.isEmpty();
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("REQUESTED TOKENS [");
        if(!mRequestedTokens.isEmpty())
        {
            sb.append(Joiner.on(",").join(mRequestedTokens));
        }

        if(!mRequestedTokens.isEmpty() && !mUnrecognizedRequestedTokens.isEmpty())
        {
            sb.append(",");
        }

        if(!mUnrecognizedRequestedTokens.isEmpty())
        {
            sb.append("UNRECOGNIZED(").append(Joiner.on(",").join(mUnrecognizedRequestedTokens)).append(")");
        }

        sb.append("]");
        return sb.toString();
    }
}
