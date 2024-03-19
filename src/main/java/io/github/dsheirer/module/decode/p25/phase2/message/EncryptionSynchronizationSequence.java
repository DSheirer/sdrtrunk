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

package io.github.dsheirer.module.decode.p25.phase2.message;

import io.github.dsheirer.audio.codec.mbe.IEncryptionSyncParameters;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import java.util.Collections;
import java.util.List;

/**
 * APCO25 Phase II Encryption Synchronization Sequence (ESS).  Provides encryption algorithm, key ID and message
 * indicator (key generator fill/seed sequence).
 */
public class EncryptionSynchronizationSequence extends P25P2Message implements IEncryptionSyncParameters
{
    private static final IntField ALGORITHM = IntField.length8(0);
    private static final IntField KEY_ID = IntField.length16(8);
    private static final IntField MESSAGE_INDICATOR_1 = IntField.length8(24);
    private static final IntField MESSAGE_INDICATOR_2 = IntField.length8(32);
    private static final IntField MESSAGE_INDICATOR_3 = IntField.length8(40);
    private static final IntField MESSAGE_INDICATOR_4 = IntField.length8(48);
    private static final IntField MESSAGE_INDICATOR_5 = IntField.length8(56);
    private static final IntField MESSAGE_INDICATOR_6 = IntField.length8(64);
    private static final IntField MESSAGE_INDICATOR_7 = IntField.length8(72);
    private static final IntField MESSAGE_INDICATOR_8 = IntField.length8(80);
    private static final IntField MESSAGE_INDICATOR_9 = IntField.length8(88);

    private EncryptionKeyIdentifier mEncryptionKey;

    public EncryptionSynchronizationSequence(CorrectedBinaryMessage message, int timeslot, long timestamp)
    {
        super(message, 0, timeslot, timestamp);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("TS").append(getTimeslot());
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
        return getMessage().getInt(ALGORITHM);
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
        return getMessage().getInt(KEY_ID);
    }

    /**
     * Encryption message indicator or fill/seed sequence.
     */
    @Override
    public String getMessageIndicator()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getIntAsHex(MESSAGE_INDICATOR_1, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_2, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_3, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_4, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_5, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_6, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_7, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_8, 2));
        sb.append(getIntAsHex(MESSAGE_INDICATOR_9, 2));
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.singletonList(getEncryptionKey());
    }
}
