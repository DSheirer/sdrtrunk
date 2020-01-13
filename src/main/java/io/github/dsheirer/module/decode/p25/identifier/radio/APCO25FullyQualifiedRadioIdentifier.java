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

package io.github.dsheirer.module.decode.p25.identifier.radio;

import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * Fully Qualified Radio Identifier (talkgroup) that includes WACN, System, and Radio Address.
 */
public class APCO25FullyQualifiedRadioIdentifier extends FullyQualifiedRadioIdentifier
{
    public APCO25FullyQualifiedRadioIdentifier(int wacn, int system, int id, Role role)
    {
        super(wacn, system, id, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    public static APCO25FullyQualifiedRadioIdentifier createFrom(int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedRadioIdentifier(wacn, system, id, Role.FROM);
    }

    public static APCO25FullyQualifiedRadioIdentifier createTo(int wacn, int system, int id)
    {
        return new APCO25FullyQualifiedRadioIdentifier(wacn, system, id, Role.TO);
    }
}
