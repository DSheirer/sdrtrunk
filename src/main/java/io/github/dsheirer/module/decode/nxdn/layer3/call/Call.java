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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNEncryptionKey;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallOption;
import io.github.dsheirer.protocol.Protocol;

/**
 * Base call message
 */
public abstract class Call extends CallControl
{
    protected static final IntField CALL_OPTION = IntField.length5(OCTET_2 + 3);
    private static final IntField CIPHER_TYPE = IntField.length2(OCTET_7);
    private static final IntField KEY_ID = IntField.length6(OCTET_7 + 2);

    private EncryptionKeyIdentifier mEncryptionKeyIdentifier;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public Call(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Call options for this call.
     */
    public abstract CallOption getCallOption();

    /**
     * Encryption algorithm and key ID.
     */
    public EncryptionKeyIdentifier getEncryptionKeyIdentifier()
    {
        if(mEncryptionKeyIdentifier == null)
        {
            mEncryptionKeyIdentifier = EncryptionKeyIdentifier.create(Protocol.NXDN, NXDNEncryptionKey.create(getMessage().getInt(CIPHER_TYPE), getMessage().getInt(KEY_ID)));
        }

        return mEncryptionKeyIdentifier;
    }
}
