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

package io.github.dsheirer.module.decode.ip.hytera.sds;

import com.google.common.base.Joiner;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.ip.IHeader;
import java.util.ArrayList;
import java.util.List;

/**
 * Message parser/decoder for Hytera run-length encoded payload message carried in Proprietary Data packet sequences.
 */
public class HyteraTokenHeader implements IHeader
{
    private static int[] TWO_BYTES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
    private static int[] FOUR_BYTES = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19,
            20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};

    private CorrectedBinaryMessage mMessage;
    private List<HyteraToken> mParsedTokens = new ArrayList<>();

    /**
     * Constructs an instance.
     * @param message assembled from data blocks.
     */
    public HyteraTokenHeader(CorrectedBinaryMessage message)
    {
        mMessage = message;
        parse();
    }

    @Override
    public boolean isValid()
    {
        return false;
    }

    @Override
    public int getLength()
    {
        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("HYTERA TOKEN HEADER: ").append(Joiner.on(",").join(getParsedTokens()));
        return sb.toString();
    }

    /**
     * Indicates if this header contains enough tokens to create an SMS message
     */
    public boolean isSMSMessage()
    {
        return hasTokens(HyteraTokenType.MESSAGE_HEADER, HyteraTokenType.ENCODING, HyteraTokenType.PAYLOAD);
    }

    /**
     * Indicates if this message has all of the specified tokens.
     * @param types one or more tokens.
     * @return true if all tokens are present in this payload.
     */
    public boolean hasTokens(HyteraTokenType ... types)
    {
        boolean hasToken = true;

        for(HyteraTokenType type: types)
        {
            hasToken &= hasToken(type);
        }

        return hasToken;
    }

    /**
     * Indicates if this payload has the specified token type.
     * @param type of token to check
     * @return true if this payload has a non-null instance of the token type
     */
    public boolean hasToken(HyteraTokenType type)
    {
        return getTokenByType(type) != null;
    }

    /**
     * Retrieves tokens by type.
     * @param type of token
     * @return token or null
     */
    public HyteraToken getTokenByType(HyteraTokenType type)
    {
        for(HyteraToken token: mParsedTokens)
        {
            if(token.getTokenType() == type)
            {
                return token;
            }
        }

        return null;
    }

    /**
     * Fields parsed from this SDS message
     * @return fields
     */
    public List<HyteraToken> getParsedTokens()
    {
        return mParsedTokens;
    }

    private void parse()
    {
        int offset = 0;

        while(offset < mMessage.length())
        {
            int opcode = mMessage.getInt(TWO_BYTES, offset);
            int length = 2 * 8; //Opcode length in bits

            HyteraTokenType token = HyteraTokenType.fromOpcode(opcode);

            if(token != HyteraTokenType.UNKNOWN)
            {
                if(token.isFixedLength())
                {
                    length += token.getFieldLength() * 8;  //Content length
                }
                else
                {
                    int contentLength = mMessage.getInt(TWO_BYTES, offset + length);
                    length += 2 * 8; //Content length 4 bytes
                    length += contentLength * 8;  //Actual content
                }

                HyteraToken field = HyteraTokenFactory.getField(token, mMessage.getSubMessage(offset, offset + length + 1));
                offset += length;
                mParsedTokens.add(field);
            }
            else
            {
                CorrectedBinaryMessage remainder = mMessage.getSubMessage(offset, mMessage.length() + 1);
                HyteraToken unknown = HyteraTokenFactory.getField(HyteraTokenType.UNKNOWN, remainder);
                mParsedTokens.add(unknown);
                offset = mMessage.length();
            }
        }
    }
}
