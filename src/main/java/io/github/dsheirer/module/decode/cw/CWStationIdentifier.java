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

package io.github.dsheirer.module.decode.cw;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Station identifier (call sign) decoded from a Morse code (CW) station ID.  The station is the transmitting (FROM)
 * entity.
 */
public class CWStationIdentifier extends StringIdentifier
{
    /**
     * Constructs an instance
     * @param callSign decoded
     */
    public CWStationIdentifier(String callSign)
    {
        super(callSign, IdentifierClass.USER, Form.SITE, Role.FROM);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.CW;
    }
}
