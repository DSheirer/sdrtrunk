/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.mobility;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNFullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.AuthenticationOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;

/**
 * Base authentication message
 */
public abstract class Authentication extends NXDNLayer3Message
{
    private static final IntField AUTHENTICATION_OPTION = IntField.length8(OCTET_1);
    private static final IntField SOURCE_ID = IntField.length16(OCTET_2);
    private static final IntField DESTINATION_ID = IntField.length16(OCTET_4);
    private NXDNRadioIdentifier mSourceIdentifier;
    private NXDNRadioIdentifier mDestinationIdentifier;
    private AuthenticationOption mAuthenticationOption;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public Authentication(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    protected abstract int getLocationIdOffset();

    public AuthenticationOption getAuthenticationOption()
    {
        if(mAuthenticationOption == null)
        {
            mAuthenticationOption = new AuthenticationOption(getMessage().getInt(AUTHENTICATION_OPTION));
        }

        return mAuthenticationOption;
    }

    public LocationID getLocationID()
    {
        return new LocationID(getMessage(), getLocationIdOffset(), true);
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null)
        {
            if(getAuthenticationOption().hasLocationID())
            {
                mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                        getMessage().getInt(SOURCE_ID));
            }
            else
            {
                mSourceIdentifier = NXDNRadioIdentifier.createFrom(getMessage().getInt(SOURCE_ID));
            }
        }

        return mSourceIdentifier;
    }

    /**
     * Destination identifier
     * @return destination identifier
     */
    public NXDNRadioIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            mDestinationIdentifier = NXDNRadioIdentifier.createTo(getMessage().getInt(DESTINATION_ID));
        }

        return mDestinationIdentifier;
    }
}
