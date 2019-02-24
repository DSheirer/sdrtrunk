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

package io.github.dsheirer.identifier.encryption;

import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;

public class EncryptionKeyIdentifier extends Identifier<EncryptionKey>
{
    public EncryptionKeyIdentifier(EncryptionKey value, IdentifierClass identifierClass, Form form, Role role)
    {
        super(value, identifierClass, form, role);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    public boolean isEncrypted()
    {
        return getValue() != null && getValue().isEncrypted();
    }

    public static EncryptionKeyIdentifier create(EncryptionKey encryptionKey)
    {
        return new EncryptionKeyIdentifier(encryptionKey, IdentifierClass.USER, Form.ENCRYPTION_KEY, Role.ANY);
    }
}
