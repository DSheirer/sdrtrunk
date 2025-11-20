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
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.RegistrationOption;

/**
 * Base registration message
 */
public abstract class Registration extends NXDNLayer3Message
{
    private static final IntField REGISTRATION_OPTION = IntField.length5(OCTET_1);
    protected static final IntField UNIT_ID = IntField.length16(OCTET_4);
    private static final IntField GROUP_ID = IntField.length16(OCTET_6);
    private static final int LOCATION_ID_OFFSET = OCTET_1 + 5;
    protected NXDNFullyQualifiedRadioIdentifier mSourceIdentifier;
    private NXDNTalkgroupIdentifier mGroupIdentifier;
    private RegistrationOption mRegistrationOption;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type
     * @param ran value
     * @param lich info
     */
    public Registration(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    public RegistrationOption getRegistrationOption()
    {
        if(mRegistrationOption == null)
        {
            mRegistrationOption = new RegistrationOption(getMessage().getInt(REGISTRATION_OPTION));
        }

        return mRegistrationOption;
    }

    public LocationID getLocationID()
    {
        return new LocationID(getMessage(), LOCATION_ID_OFFSET, true);
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNFullyQualifiedRadioIdentifier getRadio()
    {
        if(mSourceIdentifier == null)
        {
            mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                    getMessage().getInt(UNIT_ID));
        }

        return mSourceIdentifier;
    }

    /**
     * Destination identifier, either talkgroup or radio.
     * @return destination identifier
     */
    public NXDNTalkgroupIdentifier getGroup()
    {
        if(mGroupIdentifier == null)
        {
            mGroupIdentifier = NXDNTalkgroupIdentifier.createTo(getMessage().getInt(GROUP_ID));
        }

        return mGroupIdentifier;
    }
}
