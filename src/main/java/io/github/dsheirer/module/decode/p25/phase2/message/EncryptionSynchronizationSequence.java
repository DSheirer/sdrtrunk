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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.audio.codec.mbe.IEncryptionSyncParameters;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;

import java.util.ArrayList;
import java.util.List;

/**
 * APCO25 Phase II Encryption Synchronization Sequence (ESS).  Provides encryption algorithm, key ID and message
 * indicator (key generator fill/seed sequence).
 */
public class EncryptionSynchronizationSequence extends P25P2Message implements IEncryptionSyncParameters
{
    private static final int[] ALGORITHM = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] KEY_ID = new int[]{8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MESSAGE_INDICATOR_1 = new int[]{24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] MESSAGE_INDICATOR_2 = new int[]{32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] MESSAGE_INDICATOR_3 = new int[]{40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] MESSAGE_INDICATOR_4 = new int[]{48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] MESSAGE_INDICATOR_5 = new int[]{56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] MESSAGE_INDICATOR_6 = new int[]{64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] MESSAGE_INDICATOR_7 = new int[]{72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] MESSAGE_INDICATOR_8 = new int[]{80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] MESSAGE_INDICATOR_9 = new int[]{88, 89, 90, 91, 92, 93, 94, 95};

    private BinaryMessage mMessage;
    private EncryptionKeyIdentifier mEncryptionKey;
    private int mTimeslot;

    public EncryptionSynchronizationSequence(BinaryMessage message, int timeslot, long timestamp)
    {
        super(timestamp);
        mMessage = message;
        mTimeslot = timeslot;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(mTimeslot);
        sb.append(" ESS ").append(getEncryptionKey().toString());

        if(isEncrypted())
        {
            sb.append(" MI:").append(getMessageIndicator());
        }

        return sb.toString();
    }

    /**
     * Encryption key identifier that identifies the algorithm and specific key identifier.
     *
     * NOTE: check isValid() before accessing this method, otherwise this method can return a null value.
     *
     * @return encryption key or null
     */
    @Override
    public EncryptionKeyIdentifier getEncryptionKey()
    {
        if(mEncryptionKey == null && isValid())
        {
            mEncryptionKey = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(getAlgorithmId(), getEncryptionKeyId()));
        }

        return mEncryptionKey;
    }

    /**
     * APCO-25 encryption algorithm numeric identifier
     */
    public int getAlgorithmId()
    {
        return mMessage.getInt(ALGORITHM);
    }

    /**
     * Indicates if the audio associated with this sequence is encrypted.
     */
    public boolean isEncrypted()
    {
        return getEncryptionKey() != null && getEncryptionKey().isEncrypted();
    }

    /**
     * Encryption key identifier
     */
    public int getEncryptionKeyId()
    {
        return mMessage.getInt(KEY_ID);
    }

    /**
     * Encryption message indicator or fill/seed sequence.
     */
    @Override
    public String getMessageIndicator()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_1, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_2, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_3, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_4, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_5, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_6, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_7, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_8, 2).toUpperCase());
        sb.append(mMessage.getHex(MESSAGE_INDICATOR_9, 2).toUpperCase());
        return sb.toString();
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public int getTimeslot()
    {
        return mTimeslot;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(getEncryptionKey());
        return identifiers;
    }
}
