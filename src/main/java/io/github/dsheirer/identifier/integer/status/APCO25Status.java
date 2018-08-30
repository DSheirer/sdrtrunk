/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.identifier.integer.status;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.AbstractIntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO-25 Unit or User status
 */
public class APCO25Status extends AbstractIntegerIdentifier
{
    private Role mRole;

    /**
     * Constructs an APCO-25 status
     * @param status value
     * @param role of the user/unit that the status applies to
     */
    public APCO25Status(int status, Role role)
    {
        super(status);
        mRole = role;
    }

    @Override
    public Form getForm()
    {
        return Form.STATUS;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Role indicating who/what the status value applies to
     */
    public Role getRole()
    {
        return mRole;
    }

    /**
     * Creates a unit status
     * @param status
     * @return
     */
    public static APCO25Status createUnitStatus(int status)
    {
        return new APCO25Status(status, Role.UNIT);
    }

    /**
     * Creates a User status
     */
    public static APCO25Status createUserStatus(int status)
    {
        return new APCO25Status(status, Role.USER);
    }
}
