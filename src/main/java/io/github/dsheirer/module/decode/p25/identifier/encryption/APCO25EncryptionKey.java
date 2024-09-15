/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.identifier.encryption;

import io.github.dsheirer.identifier.encryption.EncryptionKey;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

public class APCO25EncryptionKey extends EncryptionKey
{
    public APCO25EncryptionKey(int algorithm, int keyId)
    {
        super(algorithm, keyId);
    }

    public Encryption getEncryptionAlgorithm()
    {
        return Encryption.fromValue(getAlgorithm());
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isEncrypted())
        {
            Encryption encryption = getEncryptionAlgorithm();

            if(encryption == Encryption.UNKNOWN)
            {
                sb.append("ALGORITHM ID:").append(getAlgorithm());
            }
            else
            {
                sb.append("ENCRYPTION:").append(getEncryptionAlgorithm());
            }

            sb.append(" KEY:").append(getKey());
        }
        else
        {
            sb.append("NO ENCRYPTION");
        }
        return sb.toString();
    }

    @Override
    public boolean isEncrypted()
    {
        return getEncryptionAlgorithm() != Encryption.UNENCRYPTED;
    }

    /**
     * Creates a new APCO-25 encryption algorithm
     */
    public static APCO25EncryptionKey create(int algorithm, int keyId)
    {
        return new APCO25EncryptionKey(algorithm, keyId);
    }

    /**
     * Creates a new APCO-25 encryption algorithm
     */
    public static APCO25EncryptionKey create(Encryption encryption, int keyId)
    {
        return create(encryption.getValue(), keyId);
    }
}
