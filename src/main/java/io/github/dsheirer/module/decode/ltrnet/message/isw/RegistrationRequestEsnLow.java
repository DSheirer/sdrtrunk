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
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.esn.ESNIdentifier;
import io.github.dsheirer.module.decode.ltrnet.LtrNetMessageType;
import io.github.dsheirer.protocol.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * ESN Low Message.  ESN is transmitted over two messages with the ESNHigh message containing the high-order 16 bits
 * and the ESNLow message containing the low-order 16 bits.
 */
public class RegistrationRequestEsnLow extends LtrNetIswMessage
{
    private RegistrationRequestEsnHigh mRegistrationRequestEsnHigh;
    private ESNIdentifier mESNIdentifier;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs an ESN-High message
     */
    public RegistrationRequestEsnLow(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public LtrNetMessageType getLtrNetMessageType()
    {
        return LtrNetMessageType.ISW_REGISTRATION_REQUEST_ESN_LOW;
    }

    /**
     * Sets the corresponding ESN high message that can be used to create the full ESN value
     */
    public void setESNHighMessage(RegistrationRequestEsnHigh message)
    {
        if(message != null)
        {
            mRegistrationRequestEsnHigh = message;
        }
    }

    public boolean isCompleteEsn()
    {
        return mRegistrationRequestEsnHigh != null;
    }

    /**
     * ESN Identifier with at least the high-order 16-bits specified and optionally includes the low-order 16 bits.
     */
    public ESNIdentifier getESN()
    {
        if(mESNIdentifier == null)
        {
            mESNIdentifier = ESNIdentifier.create((mRegistrationRequestEsnHigh != null ? mRegistrationRequestEsnHigh.getESNHigh() : "xxxx") + getESNLow(),
                Protocol.LTR_NET, Role.FROM);
        }

        return mESNIdentifier;
    }

    public String getESNLow()
    {
        int esnLow = getMessage().getInt(SIXTEEN_BITS);

        return String.format("%04X", esnLow & 0xFFFF);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("ESN LOW: ").append(getESN());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getESN());
        }

        return mIdentifiers;
    }
}
