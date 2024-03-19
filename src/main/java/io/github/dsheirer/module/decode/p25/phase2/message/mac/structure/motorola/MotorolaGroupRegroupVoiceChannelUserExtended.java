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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.motorola;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.structure.MacStructureVendor;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;
import java.util.ArrayList;
import java.util.List;

/**
 * Motorola Group Regroup Voice Channel User Extended
 */
public class MotorolaGroupRegroupVoiceChannelUserExtended extends MacStructureVendor
{
    private static final IntField SERVICE_OPTIONS = IntField.length8(OCTET_4_BIT_24);
    private static final IntField SUPERGROUP_ADDRESS = IntField.length16(OCTET_5_BIT_32);
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_7_BIT_48);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_10_BIT_72);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_12_BIT_88 + 4);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_14_BIT_104);
    private VoiceServiceOptions mServiceOptions;
    private List<Identifier> mIdentifiers;
    private TalkgroupIdentifier mSupergroup;
    private APCO25FullyQualifiedRadioIdentifier mSource;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public MotorolaGroupRegroupVoiceChannelUserExtended(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("MOTOROLA GROUP REGROUP VOICE CHANNEL USER EXTENDED");
        if(getServiceOptions().isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        sb.append(" FM:").append(getSource());
        sb.append(" TO:").append(getSupergroup());
        return sb.toString();
    }

    /**
     * Service options for this call.
     */
    public VoiceServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Supergroup/Talkgroup active on this channel/timeslot.
     */
    public TalkgroupIdentifier getSupergroup()
    {
        if(mSupergroup == null)
        {
            mSupergroup = APCO25Talkgroup.create(getInt(SUPERGROUP_ADDRESS));
        }

        return mSupergroup;
    }

    /**
     * Indicates if this message has a non-zero radio talker identifier.
     */
    public boolean hasRadio()
    {
        return getInt(SOURCE_ADDRESS) > 0;
    }

    public FullyQualifiedRadioIdentifier getSource()
    {
        if(mSource == null)
        {
            int persona = getInt(SOURCE_ADDRESS);
            int wacn = getInt(SOURCE_SUID_WACN);
            int system = getInt(SOURCE_SUID_SYSTEM);
            int id = getInt(SOURCE_SUID_ID);
            mSource = APCO25FullyQualifiedRadioIdentifier.createFrom(persona, wacn, system, id);
        }

        return mSource;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSupergroup());

            if(hasRadio())
            {
                mIdentifiers.add(getSource());
            }
        }

        return mIdentifiers;
    }
}
