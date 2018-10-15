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
package io.github.dsheirer.identifier.integer.node;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.AbstractIntegerIdentifier;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.protocol.Protocol;

public class APCO25EncryptionKey extends AbstractNodeIdentifier
{
    private Encryption mEncryption;

    public APCO25EncryptionKey(int encryptionAlgorithm, int keyId)
    {
        super(keyId);
        mEncryption = Encryption.fromValue(encryptionAlgorithm);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public NodeType getNodeType()
    {
        return NodeType.ENCRYPTION_KEY;
    }

    /**
     * Creates a new APCO-25 encryption algorithm
     */
    public static APCO25EncryptionKey create(int alogorithm, int keyId)
    {
        return new APCO25EncryptionKey(alogorithm, keyId);
    }

    public Encryption getEncryption()
    {
        return mEncryption;
    }

    public int getKeyId()
    {
        return getValue();
    }

    @Override
    public int compareTo(IIdentifier o)
    {
        if(o instanceof AbstractIntegerIdentifier)
        {
            AbstractIntegerIdentifier other = (AbstractIntegerIdentifier) o;

            if(other.getProtocol() == getProtocol())
            {
                if(other.getRole() == getRole())
                {
                    if(o instanceof APCO25EncryptionKey)
                    {
                        APCO25EncryptionKey otherKey = (APCO25EncryptionKey) o;

                        if(otherKey.getEncryption() == getEncryption())
                        {
                            return Integer.compare(getKeyId(), otherKey.getKeyId());
                        }
                        else
                        {
                            return getEncryption().compareTo(otherKey.getEncryption());
                        }
                    }
                    else
                    {
                        return -1;
                    }
                }
                else
                {
                    return getRole().compareTo(other.getRole());
                }
            }
            else
            {
                return getProtocol().compareTo(other.getProtocol());
            }
        }

        return 0;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ENCRYPTION:").append(getEncryption());
        sb.append(" KEY:").append(getKeyId());
        return sb.toString();
    }
}
