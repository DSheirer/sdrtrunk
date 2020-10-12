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

package io.github.dsheirer.module.decode.ip.lrrp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ip.IPacket;
import io.github.dsheirer.module.decode.ip.Packet;
import io.github.dsheirer.module.decode.ip.lrrp.token.Token;
import io.github.dsheirer.module.decode.ip.lrrp.token.TokenFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Location Request/Response Protocol (LRRP) Packet
 */
public class LRRPPacket extends Packet
{
    //Constant for parsing a hexadecimal byte token identifier
    private static final int[] BYTE_VALUE = {0, 1, 2, 3, 4, 5, 6, 7};
    public static final int TOKEN_START = 16;

    private LRRPHeader mHeader;
    private List<Token> mTokens;

    /**
     * Constructs a parser for a header contained within a binary message starting at the offset.
     *
     * @param message containing the packet
     * @param offset to the packet within the message
     */
    public LRRPPacket(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("LRRP");

        for(Token token: getTokens())
        {
            sb.append(" ").append(token);
        }

        return sb.toString();
    }

    @Override
    public LRRPHeader getHeader()
    {
        if(mHeader == null)
        {
            mHeader = new LRRPHeader(getMessage(), getOffset());
        }

        return mHeader;
    }

    /**
     * List of LRRP tokens parsed from the packet
     */
    public List<Token> getTokens()
    {
        parseTokens();
        return mTokens;
    }

    private void parseTokens()
    {
        if(mTokens == null)
        {
            mTokens = new ArrayList<>();

            int characterCount = getHeader().getPayloadLength();
            int offset = getOffset() + TOKEN_START;

            while(characterCount > 0 && offset < getMessage().size())
            {
                String tokenId = getTokenIdentifier(offset);
                Token token = TokenFactory.createToken(tokenId, getMessage(), offset, characterCount);
                mTokens.add(token);
                int length = token.getByteLength();
                characterCount -= token.getByteLength();
                offset += (8 * token.getByteLength());
            }

            Collections.sort(mTokens, (o1, o2) -> o1.getTokenType().compareTo(o2.getTokenType()));
        }
    }

    /**
     * Parses the string hexadecimal value of the 8-bit value that starts at the specified offset.
     */
    private String getTokenIdentifier(int offset)
    {
        int id = getMessage().getInt(BYTE_VALUE, offset);
        return String.format("%02X", id);
    }

    @Override
    public IPacket getPayload()
    {
        //The payload is a sequence of tokens.  There is no packet payload.
        return null;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return getHeader().getIdentifiers();
    }
}
