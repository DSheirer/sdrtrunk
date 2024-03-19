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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import java.util.ArrayList;
import java.util.List;

/**
 * Radio unit monitor command - enhanced format.  Commands the target radio to initiate a group or unit-to-unit call to
 * either the source address to or the talkgroup, either clear or encrypted
 */
public class RadioUnitMonitorEnhancedCommandAbbreviated extends MacStructure
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_2_BIT_8);
    private static final IntField GROUP_ID = IntField.length24(OCTET_5_BIT_32);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_7_BIT_48);
    private static final int SM = 72; //Silent Mode
    private static final int TG = 73; //Talkgroup (true) or Radio (false) mode
    private static final IntField TRANSMIT_TIME = IntField.length8(OCTET_11_BIT_80);
    private static final IntField KEY_ID = IntField.length16(OCTET_12_BIT_88);
    private static final IntField ALGORITHM_ID = IntField.length8(OCTET_14_BIT_104);

    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private Identifier mSourceAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorEnhancedCommandAbbreviated(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" CALL ").append(isTalkgroupMode() ? " TALKGROUP:" : " RADIO:");
        sb.append(getSourceAddress());

        if(isSilentMode())
        {
            sb.append(" USE SILENT MODE");
        }

        long transmitTime = getTransmitTime();

        if(transmitTime > 0)
        {
            sb.append(" TRANSMIT TIME:").append(transmitTime).append("secs");
        }

        Encryption encryption = getEncryption();

        if(encryption != Encryption.UNENCRYPTED)
        {
            sb.append(" USE ENCRYPTION:").append(encryption);
            sb.append(" KEY:").append(getEncryptionKeyId());
        }

        return sb.toString();
    }

    /**
     * Silent Mode (true) or non-silent (false)
     */
    public boolean isSilentMode()
    {
        return getMessage().get(SM + getOffset());
    }

    /**
     * Indicates if the radio should call the group ID or the radio specified in this message.
     */
    public boolean isTalkgroupMode()
    {
        return getMessage().get(TG + getOffset());
    }

    /**
     * To radio address of the unit to be monitored
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * Source Radio or Talkgroup that will monitor the targeted radio.
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            if(isTalkgroupMode())
            {
                mSourceAddress = APCO25Talkgroup.create(getInt(GROUP_ID));
            }
            else
            {
                mSourceAddress = APCO25RadioIdentifier.createFrom(getInt(SOURCE_ADDRESS));
            }
        }

        return mSourceAddress;
    }

    /**
     * Transmit time in seconds.
     */
    public long getTransmitTime()
    {
        return getInt(TRANSMIT_TIME);
    }

    /**
     * Encryption key id to use for the callback
     */
    public int getEncryptionKeyId()
    {
        return getInt(KEY_ID);
    }

    /**
     * Encryption algorithm to use for the call-back
     */
    public Encryption getEncryption()
    {
        return Encryption.fromValue(getInt(ALGORITHM_ID));
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
