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

package io.github.dsheirer.module.decode.p25.phase1.message.ldu;

import io.github.dsheirer.audio.codec.mbe.IEncryptionSyncParameters;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import java.util.ArrayList;
import java.util.List;

/**
 * Encryption Sync Parameters from Logical Link Data Unit 2 voice frame.
 */
public class EncryptionSyncParameters implements IEncryptionSyncParameters
{
    private static final String EMPTY_MESSAGE_INDICATOR = "000000000000000000";
    private static final int[] MESSAGE_INDICATOR_1 = {0, 1, 2, 3, 4, 5, 6, 7};
    private static final int[] MESSAGE_INDICATOR_2 = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] MESSAGE_INDICATOR_3 = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] MESSAGE_INDICATOR_4 = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] MESSAGE_INDICATOR_5 = {32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] MESSAGE_INDICATOR_6 = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] MESSAGE_INDICATOR_7 = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] MESSAGE_INDICATOR_8 = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] MESSAGE_INDICATOR_9 = {64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] ALGORITHM_ID = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] KEY_ID = {80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90, 91, 92, 93, 94, 95};

    private BinaryMessage mMessage;
    private boolean mValid = true;
    private String mMessageIndicator;
    private EncryptionKeyIdentifier mEncryptionKey;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public EncryptionSyncParameters(BinaryMessage message)
    {
        mMessage = message;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            if(isEncryptedAudio())
            {
                sb.append(getEncryptionKey());
                sb.append(" MSG INDICATOR:").append(getMessageIndicator());
            }
            else
            {
                sb.append("UNENCRYPTED       ");
            }
        }
        else
        {
            sb.append("***CRC-FAIL***");
        }

        return sb.toString();
    }

    private BinaryMessage getMessage()
    {
        return mMessage;
    }

    /**
     * Indicates if this message is valid or not.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Flags this message as valid or invalid
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    public String getMessageIndicator()
    {
        if(mMessageIndicator == null)
        {
            StringBuilder sb = new StringBuilder();
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_1, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_2, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_3, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_4, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_5, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_6, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_7, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_8, 2));
            sb.append(getMessage().getHex(MESSAGE_INDICATOR_9, 2));
            mMessageIndicator = sb.toString();
        }

        return mMessageIndicator;
    }

    public EncryptionKeyIdentifier getEncryptionKey()
    {
        if(mEncryptionKey == null)
        {
            int algorithm = getMessage().getInt(ALGORITHM_ID);
            int key = getMessage().getInt(KEY_ID);

            //Detect when algorithm, key and MI are all zeros and override algorithm to set as unencrypted.
            if(algorithm == 0 && key == 0 && getMessageIndicator().contains(EMPTY_MESSAGE_INDICATOR))
            {
                algorithm = Encryption.UNENCRYPTED.getValue(); //0x80
            }

            mEncryptionKey = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(algorithm, key));
        }

        return mEncryptionKey;
    }

    /**
     * Indicates if the audio stream is encrypted
     */
    public boolean isEncryptedAudio()
    {
        return getEncryptionKey().getValue().isEncrypted();
    }

    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(isEncryptedAudio())
            {
                mIdentifiers.add(getEncryptionKey());
            }
        }

        return mIdentifiers;
    }
}
