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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FullyQualifiedTalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.reference.Encryption;
import java.util.ArrayList;
import java.util.List;

/**
 * Radio unit monitor enhance command - extended
 */
public class RadioUnitMonitorEnhancedCommandExtended extends MacStructureMultiFragment
{
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_4_BIT_24);
    private static final IntField SOURCE_SUID_WACN = IntField.range(OCTET_7_BIT_48, OCTET_7_BIT_48 + 20);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.range(68, 79);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_11_BIT_80);
    private static final int SM = OCTET_14_BIT_104; //Stealth Mode
    private static final int TG = OCTET_14_BIT_104 + 1; //Talkgroup Mode
    private static final IntField TRANSMIT_TIME = IntField.length8(OCTET_15_BIT_112);
    private static final IntField KEY_ID = IntField.length16(OCTET_16_BIT_120);
    private static final IntField ALGORITHM_ID = IntField.length8(OCTET_18_BIT_136);

    private static final IntField FRAGMENT_0_SOURCE_ADDRESS = IntField.length24(OCTET_3_BIT_16);
    private static final IntField FRAGMENT_0_TARGET_SUID_WACN = IntField.range(OCTET_6_BIT_40, OCTET_6_BIT_40 + 20);
    private static final IntField FRAGMENT_0_TARGET_SUID_SYSTEM = IntField.range(60, 71);
    private static final IntField FRAGMENT_0_TARGET_SUID_ID = IntField.length24(OCTET_10_BIT_72);
    private static final IntField FRAGMENT_0_SGID_WACN = IntField.range(OCTET_13_BIT_96, OCTET_13_BIT_96 + 20);
    private static final IntField FRAGMENT_0_SGID_SYSTEM = IntField.range(116, 127);
    private static final IntField FRAGMENT_0_SGID_ID = IntField.length24(OCTET_17_BIT_128);

    private APCO25FullyQualifiedRadioIdentifier mTargetAddress;
    private APCO25FullyQualifiedTalkgroupIdentifier mSourceGroup;
    private APCO25FullyQualifiedRadioIdentifier mSourceAddress;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RadioUnitMonitorEnhancedCommandExtended(CorrectedBinaryMessage message, int offset)
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

        if(isTalkgroupMode())
        {
            if(getSourceGroup() != null)
            {
                sb.append(" CALL TALKGROUP:").append(getSourceGroup());
            }
        }
        else
        {
            if(getSourceAddress() != null)
            {
                sb.append(" CALL RADIO:").append(getSourceAddress());
            }
        }

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
     * Target radio to be monitored
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null && hasFragment(0))
        {
            int address = getInt(TARGET_ADDRESS);
            int wacn = getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_WACN);
            int system = getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_SYSTEM);
            int id = getFragment(0).getInt(FRAGMENT_0_TARGET_SUID_ID);
            mTargetAddress = APCO25FullyQualifiedRadioIdentifier.createTo(address, wacn, system, id);
        }

        return mTargetAddress;
    }

    /**
     * Talkgroup that the targeted radio should call, if talkgroup mode is specified.
     */
    public Identifier getSourceGroup()
    {
        if(mSourceGroup == null && hasFragment(0))
        {
            int wacn = getFragment(0).getInt(FRAGMENT_0_SGID_WACN);
            int system = getFragment(0).getInt(FRAGMENT_0_SGID_SYSTEM);
            int id = getFragment(0).getInt(FRAGMENT_0_SGID_ID);
            mSourceGroup = APCO25FullyQualifiedTalkgroupIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceGroup;
    }

    /**
     * Radio that the targeted radio should call, if talkgroup mode is not specified.
     */
    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null && hasFragment(0))
        {
            int address = getFragment(0).getInt(FRAGMENT_0_SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(address, wacn, system, id);
        }

        return mSourceAddress;
    }

    /**
     * Silent mode monitoring.
     */
    public boolean isSilentMode()
    {
        return getMessage().get(SM + getOffset());
    }

    /**
     * Talkgroup mode or radio mode.
     */
    public boolean isTalkgroupMode()
    {
        return getMessage().get(TG + getOffset());
    }

    /**
     * Transmit time in seconds.
     */
    public long getTransmitTime()
    {
        return getMessage().getInt(TRANSMIT_TIME, getOffset());
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
        //Note: this has to be dynamically constructed each time to account for late-add continuation fragments.
        List<Identifier> identifiers = new ArrayList<>();

        if(getTargetAddress() != null)
        {
            identifiers.add(getTargetAddress());
        }

        if(getSourceGroup() != null)
        {
            identifiers.add(getSourceGroup());
        }

        if(getSourceAddress() != null)
        {
            identifiers.add(getSourceAddress());
        }

        return identifiers;
    }
}
