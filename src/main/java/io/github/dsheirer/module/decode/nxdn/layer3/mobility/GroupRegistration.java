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
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.GroupRegistrationOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;

/**
 * Base group registration message
 */
public abstract class GroupRegistration extends NXDNLayer3Message
{
    private static final IntField GROUP_REGISTRATION_OPTION = IntField.length8(OCTET_1);
    private static final IntField UNIT_ID = IntField.length16(OCTET_2);
    private static final IntField GROUP_ID = IntField.length16(OCTET_4);
    private NXDNRadioIdentifier mSourceIdentifier;
    private NXDNTalkgroupIdentifier mGroupIdentifier;
    private GroupRegistrationOption mRegistrationOption;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public GroupRegistration(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
    }

    protected abstract int getLocationIdOffset();

    public GroupRegistrationOption getGroupRegistrationOption()
    {
        if(mRegistrationOption == null)
        {
            mRegistrationOption = new GroupRegistrationOption(getMessage().getInt(GROUP_REGISTRATION_OPTION));
        }

        return mRegistrationOption;
    }

    public LocationID getLocationID()
    {
        return new LocationID(getMessage(), getLocationIdOffset(), true);
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNRadioIdentifier getRadio()
    {
        if(mSourceIdentifier == null)
        {
            if(getGroupRegistrationOption().hasLocationID())
            {
                mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                        getMessage().getInt(UNIT_ID));
            }
            else
            {
                mSourceIdentifier = NXDNRadioIdentifier.createFrom(getMessage().getInt(UNIT_ID));
            }
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
