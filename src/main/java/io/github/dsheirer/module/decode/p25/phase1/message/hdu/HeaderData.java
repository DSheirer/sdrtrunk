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

package io.github.dsheirer.module.decode.p25.phase1.message.hdu;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.encryption.EncryptionKeyIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.encryption.APCO25EncryptionKey;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import io.github.dsheirer.module.decode.p25.reference.Vendor;
import java.util.ArrayList;
import java.util.List;

/**
 * Header Data Unit (HDU) Information
 */
public class HeaderData
{
    private static final int[] MESSAGE_INDICATOR_A = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18,
        19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35};
    private static final int[] MESSAGE_INDICATOR_B = {36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51,
        52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71};
    private static final int[] VENDOR_ID = {72, 73, 74, 75, 76, 77, 78, 79};
    private static final int[] ALGORITHM_ID = {80, 81, 82, 83, 84, 85, 86, 87};
    private static final int[] KEY_ID = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103};
    private static final int[] TALKGROUP_ID = {104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119};

    private boolean mValid = true;
    private BinaryMessage mMessage;
    private EncryptionKeyIdentifier mEncryptionKey;
    private TalkgroupIdentifier mTalkgroup;
    private List<Identifier> mIdentifiers;

    public HeaderData(BinaryMessage message)
    {
        mMessage = message;
    }

    public BinaryMessage getMessage()
    {
        return mMessage;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(isValid())
        {
            sb.append("TALKGROUP:").append(getTalkgroup());

            Vendor vendor = getVendor();

            if(vendor != Vendor.STANDARD)
            {
                sb.append(" VENDOR:").append(vendor.getLabel());
            }

            if(isEncryptedAudio())
            {
                sb.append(" ENCRYPTION:").append(getEncryptionKey());
                sb.append(" MI:").append(getMessageIndicator());
            }
            else
            {
                sb.append(" UNENCRYPTED");
            }
        }
        else
        {
            sb.append(" **CRC FAILED**");
        }

        return sb.toString();
    }

    /**
     * Indicates if this message is valid, meaning that it has passed the CRC error detection and
     * correction check.
     */
    public boolean isValid()
    {
        return mValid;
    }

    /**
     * Sets the valid flag for this message set
     */
    public void setValid(boolean valid)
    {
        mValid = valid;
    }

    public String getMessageIndicator()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessage().getHex(MESSAGE_INDICATOR_A, 9));
        sb.append(getMessage().getHex(MESSAGE_INDICATOR_B, 9));

        return sb.toString();
    }

    public Vendor getVendor()
    {
        return Vendor.fromValue(getMessage().getInt(VENDOR_ID));
    }

    public EncryptionKeyIdentifier getEncryptionKey()
    {
        if(mEncryptionKey == null)
        {
            mEncryptionKey = EncryptionKeyIdentifier.create(APCO25EncryptionKey.create(getMessage().getInt(ALGORITHM_ID),
                getMessage().getInt(KEY_ID)));
        }

        return mEncryptionKey;
    }

    public Encryption getEncryption()
    {
        return Encryption.fromValue(getMessage().getInt(ALGORITHM_ID));
    }

    public boolean isEncryptedAudio()
    {
        return getEncryption() != Encryption.UNENCRYPTED;
    }

    public TalkgroupIdentifier getTalkgroup()
    {
        if(mTalkgroup == null)
        {
            mTalkgroup = APCO25Talkgroup.create(getMessage().getInt(TALKGROUP_ID));
        }

        return mTalkgroup;
    }

    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();

            if(isValid())
            {
                mIdentifiers.add(getTalkgroup());
                mIdentifiers.add(getEncryptionKey());
            }
        }

        return mIdentifiers;
    }
}
