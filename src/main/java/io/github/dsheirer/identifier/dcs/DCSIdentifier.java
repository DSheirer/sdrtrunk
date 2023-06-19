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

package io.github.dsheirer.identifier.dcs;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.module.decode.dcs.DCSCode;
import io.github.dsheirer.protocol.Protocol;

/**
 * Digital Coded Squelch code identifier.  This is decoded from the transmitted signal and represents the FROM role.
 */
public class DCSIdentifier extends Identifier<DCSCode>
{
    /**
     * Constructs an instance.
     * @param code detected.
     */
    public DCSIdentifier(DCSCode code)
    {
        super(code, IdentifierClass.USER, Form.TONE, Role.FROM);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DCS;
    }
}
