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

package io.github.dsheirer.module.decode.lj1200;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

public class FunctionAndReplyCodeIdentifier extends Identifier<FunctionAndReplyCode>
{
    public FunctionAndReplyCodeIdentifier(FunctionAndReplyCode functionAndReplyCode)
    {
        super(functionAndReplyCode, IdentifierClass.NETWORK, Form.LOJACK, Role.TO);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.LOJACK;
    }

    public static FunctionAndReplyCodeIdentifier create(LJ1200Message.Function function, String replyCode)
    {
        return new FunctionAndReplyCodeIdentifier(new FunctionAndReplyCode(function, replyCode));
    }
}
