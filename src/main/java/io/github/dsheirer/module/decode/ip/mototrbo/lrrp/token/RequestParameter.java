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
package io.github.dsheirer.module.decode.ip.mototrbo.lrrp.token;

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * LRRP request parameter of unknown meaning that carries a 7-bit variable length integer value.
 *
 * Start Tokens: 0x42 and 0x44
 * Total Length: 1 byte identifier plus a variable length integer value (0-5 bytes)
 *
 * Observed in Motorola P25 triggered location start requests as 0x44 0x64 (100) and 0x42 0x9C 0x10 (3600), placed
 * among the requested-field flags.  Moto.Net emits a bare 0x42 as a "trigger on GPIO" flag, so the value is treated
 * as optional and the token is rendered without a value when none follows.
 */
public class RequestParameter extends VarIntToken
{
    private TokenType mTokenType;

    /**
     * Constructs an instance of a request parameter token.
     *
     * @param tokenType of the parameter (REQUEST_PARAMETER_42 or REQUEST_PARAMETER_44)
     * @param message containing the token
     * @param offset to the start of the token
     * @param remainingCharacterCount number of payload bytes remaining, counting from the token identifier
     */
    public RequestParameter(TokenType tokenType, CorrectedBinaryMessage message, int offset, int remainingCharacterCount)
    {
        super(message, offset, remainingCharacterCount);
        mTokenType = tokenType;
    }

    @Override
    public TokenType getTokenType()
    {
        return mTokenType;
    }

    @Override
    public String toString()
    {
        if(hasValue())
        {
            return getTokenType() + ":" + getValue();
        }

        return getTokenType().toString();
    }
}
