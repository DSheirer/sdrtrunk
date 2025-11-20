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

package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNFullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ControlCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.type.Delivery;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;

/**
 * Base remote control message
 */
public abstract class RemoteControl extends CallControl
{
    private static final int FLAG_DESTINATION_IS_TALKGROUP = OCTET_2;
    private static final int FLAG_DELIVERY = OCTET_2 + 2;
    private static final IntField CONTROL_COMMAND = IntField.length5(OCTET_2 + 3);

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public RemoteControl(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Offset to the location ID and option fields.
     */
    protected abstract int getLocationOffset();

    /**
     * Source identifier.  Overrides the default method to optionally create a fully qualified identifier that includes
     * the visitor source system ID.
     */
    @Override
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null && getCallControlOption().hasLocationId() && getLocationIDOption().isSource())
        {
            mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return super.getSource();
    }

    /**
     * Destination identifier.  Overrides the default method to optionally create a fully qualified identifier that
     * includes the visitor source system ID.
     */
    @Override
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            if(isTalkgroupDestination())
            {
                mDestinationIdentifier = NXDNTalkgroupIdentifier.createTo(getMessage().getInt(IDENTIFIER_OCTET_5));
            }
            else if(getCallControlOption().hasLocationId() && getLocationIDOption().isDestination())
            {
                mDestinationIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                        getMessage().getInt(IDENTIFIER_OCTET_3));
            }
        }

        return super.getDestination();
    }

    /**
     * Indicates if the destination field is a talkgroup or unit ID.
     */
    public boolean isTalkgroupDestination()
    {
        return !getMessage().get(FLAG_DESTINATION_IS_TALKGROUP);
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
     * Location ID option
     */
    public LocationIDOption getLocationIDOption()
    {
        IntField field = IntField.length5(getLocationOffset());
        return LocationIDOption.fromValue(getMessage().getInt(field));
    }

    /**
     * Optional location ID
     */
    public LocationID getLocationID()
    {
        return new LocationID(getMessage(), getLocationOffset() + 5, true);
    }
}
