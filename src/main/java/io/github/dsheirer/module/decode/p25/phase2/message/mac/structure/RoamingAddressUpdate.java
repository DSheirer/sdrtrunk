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
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Roaming address update
 */
public class RoamingAddressUpdate extends MacStructure
{
    private static final int LAST_MESSAGE_INDICATOR = 16;
    private static final IntField MESSAGE_SEQUENCE_NUMBER = IntField.length4(OCTET_3_BIT_16 + 4);
    private static final IntField TARGET_ADDRESS = IntField.length24(OCTET_4_BIT_24);
    private static final IntField SOURCE_SUID_WACN = IntField.length20(OCTET_7_BIT_48);
    private static final IntField SOURCE_SUID_SYSTEM = IntField.length12(OCTET_9_BIT_64 + 4);
    private static final IntField SOURCE_SUID_ID = IntField.length24(OCTET_11_BIT_80);
    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;
    private APCO25FullyQualifiedRadioIdentifier mSourceSuid;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public RoamingAddressUpdate(CorrectedBinaryMessage message, int offset)
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
        sb.append(" FM:").append(getSourceSuid());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" MESSAGE SEQUENCE NUMBER:").append(getMessageSequenceNumber());
        if(isLastMessage())
        {
            sb.append(" - LAST MESSAGE");
        }
        return sb.toString();
    }

    public boolean isLastMessage()
    {
        return getMessage().get(LAST_MESSAGE_INDICATOR + getOffset());
    }

    public int getMessageSequenceNumber()
    {
        return getInt(MESSAGE_SEQUENCE_NUMBER);
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createTo(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    /**
     * From Radio Unit
     */
    public APCO25FullyQualifiedRadioIdentifier getSourceSuid()
    {
        if(mSourceSuid == null)
        {
            int wacn = getMessage().getInt(SOURCE_SUID_WACN, getOffset());
            int system = getMessage().getInt(SOURCE_SUID_SYSTEM, getOffset());
            int id = getMessage().getInt(SOURCE_SUID_ID, getOffset());
            //Fully qualified, but not aliased - reuse the ID as the persona.
            mSourceSuid = APCO25FullyQualifiedRadioIdentifier.createFrom(id, wacn, system, id);
        }

        return mSourceSuid;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceSuid());
        }

        return mIdentifiers;
    }
}
