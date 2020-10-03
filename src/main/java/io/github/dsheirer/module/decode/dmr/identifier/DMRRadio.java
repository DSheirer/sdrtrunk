/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.RadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR Radio Identifier
 */
public class DMRRadio extends RadioIdentifier
{
    public DMRRadio(Integer value, Role role)
    {
        super(value, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    /**
     * Creates a DMR TO radio identifier
     */
    public static RadioIdentifier createTo(int radioId)
    {
        return new DMRRadio(radioId, Role.TO);
    }

    /**
     * Creates a DMR FROM radio identifier
     */
    public static RadioIdentifier createFrom(int radioId)
    {
        return new DMRRadio(radioId, Role.FROM);
    }

    /**
     * Creates a DMR ANY radio identifier
     */
    public static RadioIdentifier createAny(int radioId)
    {
        return new DMRRadio(radioId, Role.ANY);
    }
}
