/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.audio.codec.mbe.IEncryptionSyncParameters;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.audio.Phase2EncryptionSyncParameters;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacOpcode;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * Call start.
 *
 * Similar to a P25 Phase 1 Header Data Unit (HDU).
 */
public class PushToTalk extends MacStructure
{
    private static int MESSAGE_INDICATOR_START = 8;
    private static int MESSAGE_INDICATOR_END = 79;
    private static int[] ALGORITHM_ID = {80, 81, 82, 83, 84, 85, 86, 87};
    private static int[] KEY_ID = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    private static int[] SOURCE_ADDRESS = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118,
        119, 120, 121, 122, 123, 124, 125, 126, 127};
    private static int[] GROUP_ADDRESS = {128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 140, 141, 142, 143};

    private EncryptionKeyIdentifier mEncryptionKey;
    private Identifier mSourceAddress;
    private Identifier mGroupAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     */
    public PushToTalk(CorrectedBinaryMessage message)
    {
        super(message, 0);
    }

    @Override
    public MacOpcode getOpcode()
    {
        return MacOpcode.PUSH_TO_TALK;
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("FROM:").append(getSourceAddress());
        sb.append(" TO:").append(getGroupAddress());

        if(isEncrypted())
        {
            sb.append(" ").append(getEncryptionKey());
            sb.append(" MI:").append(getMessageIndicator());
        }

        return sb.toString();
    }

    /**
     * Indicates if this message is encrypted and an encryption algorithm is specified.
     */
    public boolean isEncrypted()
    {
        return getEncryptionKey().getValue().isEncrypted();
    }

    /**
     * Encryption sync parameters
     */
    public IEncryptionSyncParameters getEncryptionSyncParameters()
    {
        return new Phase2EncryptionSyncParameters(getEncryptionKey(), getMessageIndicator());
    }

    /**
     * Message indicator value (encryption key generator fill sequence)
     */
    public String getMessageIndicator()
    {
        return getMessage().getHex(MESSAGE_INDICATOR_START + getOffset(), MESSAGE_INDICATOR_END + getOffset());
    }


    /**
     * Encryption key that specifies the encryption algorithm and the key ID
     * @return encryption key
     */
    public EncryptionKeyIdentifier getEncryptionKey()
    {
        if(mEncryptionKey == null)
        {
            mEncryptionKey = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(getMessage().getInt(ALGORITHM_ID, getOffset()),
                getMessage().getInt(KEY_ID, getOffset())));
        }

        return mEncryptionKey;
    }

    /**
     * Calling (source) radio identifier
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25RadioIdentifier.createFrom(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mSourceAddress;
    }

    /**
     * Called (destination) group identifier
     */
    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25Talkgroup.create(getMessage().getInt(GROUP_ADDRESS, getOffset()));
        }

        return mGroupAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSourceAddress());
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getEncryptionKey());
        }

        return mIdentifiers;
    }
}
