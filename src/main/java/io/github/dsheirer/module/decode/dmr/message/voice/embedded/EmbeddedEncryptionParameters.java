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

package io.github.dsheirer.module.decode.dmr.message.voice.embedded;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.type.EncryptionAlgorithm;

/**
 * Encryption parameters short burst payload.
 */
public class EmbeddedEncryptionParameters extends ShortBurst
{
    private static final int[] KEY = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] ALGORITHM = new int[]{8, 9, 10};

    /**
     * Constructor
     *
     * @param message containing the de-interleaved and error-corrected short burst payload.
     */
    public EmbeddedEncryptionParameters(CorrectedBinaryMessage message)
    {
        super(message);
    }

    /**
     * Encryption key ID
     * @return key ID
     */
    public int getKey()
    {
        return getMessage().getInt(KEY);
    }

    /**
     * Encryption algorithm value
     * @return algorithm (0 - 7).
     */
    public int getAlgorithmValue()
    {
        return getMessage().getInt(ALGORITHM);
    }

    /**
     * Encryption algorithm.
     * @return algorithm
     */
    public EncryptionAlgorithm getAlgorithm()
    {
        int algorithm = getAlgorithmValue();

        return switch(algorithm)
        {
            case 1 -> EncryptionAlgorithm.DMRA_RC4;
            case 4 -> EncryptionAlgorithm.DMRA_AES128;
            case 5 -> EncryptionAlgorithm.DMRA_AES256;
            default -> EncryptionAlgorithm.UNKNOWN;
        };
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("ENCRYPTION ALGORITHM:");
        if(getAlgorithm() == EncryptionAlgorithm.UNKNOWN)
        {
            sb.append(getAlgorithmValue());
        }
        else
        {
            sb.append(getAlgorithm());
        }
        sb.append(" KEY:").append(getKey());
        return sb.toString();
    }
}
