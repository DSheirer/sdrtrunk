/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.identifier;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.protocol.Protocol;

/**
 * DMR Network Identifier
 */
public class DMRNetwork extends IntegerIdentifier
{
    /**
     * Creates an instance
     * @param value of the network
     */
    public DMRNetwork(int value)
    {
        super(value, IdentifierClass.NETWORK, Form.NETWORK, Role.BROADCAST);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DMR;
    }

    public static DMRNetwork create(int network)
    {
        return new DMRNetwork(network);
    }
}
