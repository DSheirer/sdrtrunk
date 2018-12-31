/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.ltrnet.message.isw;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.module.decode.ltrnet.identifier.LtrNetRadioIdentifier;

import java.util.ArrayList;
import java.util.List;

/**
 * ESN Low Message.  ESN is transmitted over two messages with the ESNHigh message containing the high-order 16 bits
 * and the ESNLow message containing the low-order 16 bits.
 */
public class IswUniqueId extends LtrNetIswMessage
{
    private LtrNetRadioIdentifier mLtrNetRadioIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an ESN-High message
     */
    public IswUniqueId(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.ISW_UNIQUE_ID;
    }

    public LtrNetRadioIdentifier getUniqueID()
    {
        if(mLtrNetRadioIdentifier == null)
        {
            mLtrNetRadioIdentifier = LtrNetRadioIdentifier.createFrom(getMessage().getInt(SIXTEEN_BITS));
        }

        return mLtrNetRadioIdentifier;
    }


    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("RADIO UNIQUE ID: ").append(getUniqueID());
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
