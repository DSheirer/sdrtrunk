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
package io.github.dsheirer.module.decode.ltrnet.message.osw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.edac.CRCLTR;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.identifier.LtrNetRadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Registration and assignment of Unique ID to a radio.
 */
public class RegistrationAccept extends LtrNetOswMessage
{
    private LtrNetRadioIdentifier mLtrNetRadioIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an ESN-High message
     */
    public RegistrationAccept(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.OSW_REGISTRATION_ACCEPT;
    }

    public LtrNetRadioIdentifier getUniqueID()
    {
        if(mLtrNetRadioIdentifier == null)
        {
            mLtrNetRadioIdentifier = LtrNetRadioIdentifier.createTo(getMessage().getInt(SIXTEEN_BITS));
        }

        return mLtrNetRadioIdentifier;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if(!isValid())
        {
            sb.append("[CRC FAIL: ").append(CRCLTR.getCRCReason(mMessage, getMessageDirection())).append("] ");
        }
        sb.append("REGISTRATION ACCEPT - RADIO UNIQUE ID: ").append(getUniqueID());
        sb.append(" MSG:").append(getMessage().toString());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getUniqueID());
        }

        return mIdentifiers;
    }
}
