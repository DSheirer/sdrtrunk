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
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;

/**
 * Call message with optional location field for fully qualified source/destination identifiers.
 */
public abstract class CallWithOptionalLocation extends Call
{
    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public CallWithOptionalLocation(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    /**
     * Offset to the location ID and option field within the message.  This is implemented by the subclass.
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
        if(mDestinationIdentifier == null && getCallControlOption().hasLocationId() && getLocationIDOption().isDestination())
        {
            mDestinationIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return super.getDestination();
    }

    /**
     * Location ID Option field.
     */
    public LocationIDOption getLocationIDOption()
    {
        IntField field = IntField.length2(getLocationOffset());
        return LocationIDOption.fromValue(getMessage().getInt(field));
    }

    /**
     * Creates a location ID field parser.
     */
    public LocationID getLocationID()
    {
        return new LocationID(getMessage(), getLocationOffset() + 5, true);
    }
}
