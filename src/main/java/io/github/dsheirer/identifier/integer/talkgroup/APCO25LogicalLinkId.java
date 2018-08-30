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
package io.github.dsheirer.identifier.integer.talkgroup;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.AbstractIntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * APCO25 Talkgroup Identifier with a FROM role.
 */
public class APCO25LogicalLinkId extends AbstractIntegerIdentifier
{
    /**
     * Constructs an APCO25 Talkgroup Identifier with a FROM role.
     * @param value of the talkgroup
     */
    public APCO25LogicalLinkId(int value)
    {
        super(value);
    }

    @Override
    public Role getRole()
    {
        return Role.FROM;
    }

    @Override
    public Form getForm()
    {
        return Form.LOGICAL_LINK_ID;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    /**
     * Creates a FROM APCO-25 talkgroup identifier
     */
    public static IIdentifier create(int llid)
    {
        return new APCO25LogicalLinkId(llid);
    }
}
