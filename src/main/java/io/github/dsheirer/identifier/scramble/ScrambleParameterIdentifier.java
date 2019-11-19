/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.identifier.scramble;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 Phase II Scramble Parameters Identifier
 */
public class ScrambleParameterIdentifier extends Identifier<ScrambleParameters>
{
    /**
     * Constructs an instance
     * @param value to wrap in this instance
     */
    private ScrambleParameterIdentifier(ScrambleParameters value)
    {
        super(value, IdentifierClass.NETWORK, Form.SCRAMBLE_PARAMETERS, Role.BROADCAST);
    }

    /**
     * P25P2 Protocol
     */
    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25_PHASE2;
    }

    /**
     * Creates an instance
     * @param scrambleParameters to wrap in an identifier
     */
    public static ScrambleParameterIdentifier create(ScrambleParameters scrambleParameters)
    {
        return new ScrambleParameterIdentifier(scrambleParameters);
    }
}

