/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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
package io.github.dsheirer.module.decode.nxdn.identifier;

import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CipherType;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

/**
 * NXDN Encryption Parameters
 */
public class NXDNEncryptionKey extends EncryptionKey
{
    /**
     * Constructs an instance
     * @param algorithm id
     * @param keyId value
     */
    public NXDNEncryptionKey(int algorithm, int keyId)
    {
        super(algorithm, keyId);
    }

    /**
     * Cipher type for the call.
     * @return type
     */
    public CipherType getCipherType()
    {
        return CipherType.fromValue(getAlgorithm());
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isEncrypted())
        {
            sb.append("ENCRYPTION:").append(getCipherType());
            sb.append(" KEY:").append(getKey());
        }
        else
        {
            sb.append("UNENCRYPTED");
        }

        return sb.toString();
    }

    @Override
    public boolean isEncrypted()
    {
        return getCipherType() != CipherType.UNENCRYPTED;
    }

    /**
     * Creates a new NXDN encryption algorithm
     */
    public static NXDNEncryptionKey create(int algorithm, int keyId)
    {
        return new NXDNEncryptionKey(algorithm, keyId);
    }

    /**
     * Creates a new NXDN encryption algorithm
     */
    public static NXDNEncryptionKey create(Encryption encryption, int keyId)
    {
        return create(encryption.getValue(), keyId);
    }
}
