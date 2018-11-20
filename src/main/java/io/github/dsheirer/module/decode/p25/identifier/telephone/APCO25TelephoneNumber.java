/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.p25.identifier.telephone;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.string.StringIdentifier;
import io.github.dsheirer.protocol.Protocol;

public class APCO25TelephoneNumber extends StringIdentifier
{
    public APCO25TelephoneNumber(String telephoneNumber, Role role)
    {
        super(telephoneNumber, IdentifierClass.USER, Form.TELEPHONE_NUMBER, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates a new APCO-25 telephone number
     */
    public static Identifier createFrom(String telephoneNumber)
    {
        return new APCO25TelephoneNumber(telephoneNumber, Role.FROM);
    }

    /**
     * Creates a new APCO-25 telephone number
     */
    public static Identifier createTo(String telephoneNumber)
    {
        return new APCO25TelephoneNumber(telephoneNumber, Role.TO);
    }

    /**
     * Creates a new APCO-25 telephone number
     */
    public static Identifier createAny(String telephoneNumber)
    {
        return new APCO25TelephoneNumber(telephoneNumber, Role.ANY);
    }
}
