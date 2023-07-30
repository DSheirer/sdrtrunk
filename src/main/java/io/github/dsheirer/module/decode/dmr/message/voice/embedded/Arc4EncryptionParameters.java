/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

/**
 * Encryption parameters short burst payload.
 */
public class Arc4EncryptionParameters extends ShortBurst
{
    private static final int[] KEY = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] ALGORITHM = new int[]{8, 9, 10};

    /**
     * Constructor
     *
     * @param message containing the de-interleaved and error-corrected short burst payload.
     */
    public Arc4EncryptionParameters(CorrectedBinaryMessage message)
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
     * Encryption algorithm
     * @return algorithm (0 - 7).  1 = ARC4
     */
    public int getAlgorithmValue()
    {
        return getMessage().getInt(ALGORITHM);
    }

    public String getAlgorithm()
    {
        int algorithm = getAlgorithmValue();

        if(algorithm == 1)
        {
            return "EP/ARC4";
        }

        return "ALGORITHM:" + algorithm;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ENCRYPTION:").append(getAlgorithm());
        sb.append(" KEY:").append(getKey());
        return sb.toString();
    }
}
