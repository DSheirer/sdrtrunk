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
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ControlCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ControlParameter;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Delivery;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import java.util.List;

/**
 * Request for remote control of an SU radio
 */
public class RemoteControlRequest extends CallControl
{
    private static final int FLAG_DESTINATION_IS_TALKGROUP = OCTET_2;
    private static final int FLAG_DELIVERY = OCTET_2 + 2;
    private static final IntField CONTROL_COMMAND = IntField.length5(OCTET_2 + 3);
    private static final IntField CONTROL_PARAMETER = IntField.length16(OCTET_7);
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_9);
    private static final int OFFSET_LOCATION_ID = OCTET_9 + 5;
    private ControlParameter mControlParameter;
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public RemoteControlRequest(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_REMOTE_CONTROL_REQUEST;
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

        sb.append(getCallType()).append(" REQUEST REMOTE CONTROL ").append(getControlParameters().interpret(getControlCommand()));
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());

        if(getCallControlOption().hasLocationId())
        {
            sb.append(" ").append(getLocationIdOption()).append(" LOCATION:").append(getLocationId());
        }

        sb.append(" ").append(getDelivery()).append(" DELIVERY");

        return sb.toString();
    }


    /**
     * Indicates if the destination field is a talkgroup or unit ID.
     */
    public boolean isTalkgroupDestination()
    {
        return getMessage().get(FLAG_DESTINATION_IS_TALKGROUP);
    }

    /**
     * Delivery method, confirmed or unconfirmed.
     */
    public Delivery getDelivery()
    {
        return getMessage().get(FLAG_DELIVERY) ? Delivery.CONFIRMED : Delivery.UNCONFIRMED;
    }

    /**
     * Commands used for remote control
     */
    public ControlCommand getControlCommand()
    {
        return ControlCommand.fromValue(getMessage().getInt(CONTROL_COMMAND));
    }

    /**
     * Control parameters.
     */
    public ControlParameter getControlParameters()
    {
        if(mControlParameter == null)
        {
            mControlParameter = new ControlParameter(getMessage().getInt(CONTROL_PARAMETER));
        }

        return mControlParameter;
    }

    /**
     * Location ID option
     */
    public LocationIDOption getLocationIdOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Optional location ID
     */
    public LocationID getLocationId()
    {
        if(mLocationID == null)
        {
            mLocationID = new LocationID(getMessage(), OFFSET_LOCATION_ID, true);
        }

        return mLocationID;
    }

    /**
     * Destination identifier, either talkgroup or radio.
     *
     * @return destination identifier
     */
    @Override
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            if(isTalkgroupDestination())
            {
                mDestinationIdentifier = NXDNTalkgroupIdentifier.to(getMessage().getInt(IDENTIFIER_OCTET_5));
            }
            else
            {
                mDestinationIdentifier = NXDNRadioIdentifier.to(getMessage().getInt(IDENTIFIER_OCTET_5));
            }
        }

        return mDestinationIdentifier;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination());
    }
}
