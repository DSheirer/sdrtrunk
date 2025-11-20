/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.bits.LongField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallControlOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ControlCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import java.util.List;

/**
 * Trunking controller request to remotely control an SU radio, identified by ESN
 */
public class RemoteControlRequestWithESN extends NXDNLayer3Message
{
    private static final IntField CC_OPTION = IntField.length8(OCTET_1);
    private static final IntField CONTROL_COMMAND_2 = IntField.length5(OCTET_2 + 3);
    private static final IntField AUTHENTICATION_PARAMETER = IntField.length8(OCTET_3);
    private static final LongField AUTHENTICATION_VALUE = LongField.range(OCTET_7, OCTET_14 - 1);
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_14);
    private static final int OFFSET_LOCATION_ID = OCTET_14 + 5;
    private CallControlOption mCallControlOption;
    private NXDNRadioIdentifier mDestinationIdentifier;
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public RemoteControlRequestWithESN(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_REMOTE_CONTROL_REQUEST_WITH_ESN;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        sb.append("REMOTE CONTROL REQUEST WITH ESN ").append(getCommand());
        sb.append(" TO:").append(getDestination());
        sb.append(" AUTHENTICATION PARAMETER:").append(getAuthenticationParameter());
        sb.append(" VALUE:").append(getAuthenticationValue());

        if(getCallControlOption().hasLocationId())
        {
            sb.append(" ").append(getLocationIDOption()).append(" LOCATION:").append(getLocationIDOption());
        }

        return sb.toString();
    }


    /**
     * Call control options for the call
     * @return options
     */
    public CallControlOption getCallControlOption()
    {
        if(mCallControlOption == null)
        {
            mCallControlOption = new CallControlOption(getMessage().getInt(CC_OPTION));
        }

        return mCallControlOption;
    }

    /**
     * Control command.
     */
    public ControlCommand getCommand()
    {
        return ControlCommand.fromValue(getMessage().getInt(CONTROL_COMMAND_2));
    }

    /**
     * Destination identifier, either talkgroup or radio.
     * @return destination identifier
     */
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            mDestinationIdentifier = NXDNRadioIdentifier.to(getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return mDestinationIdentifier;
    }

    /**
     * Authentication parameter
     */
    public String getAuthenticationParameter()
    {
        return getMessage().getHex(AUTHENTICATION_PARAMETER);
    }

    /**
     * Authentication value.
     */
    public String getAuthenticationValue()
    {
        return getMessage().getHex(AUTHENTICATION_VALUE);
    }

    /**
     * Location ID option
     */
    public LocationIDOption getLocationIDOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Optional location ID partial.
     */
    public LocationID getLocationID()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), OFFSET_LOCATION_ID, true);
        }

        return mLocationID;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getDestination());
    }
}
