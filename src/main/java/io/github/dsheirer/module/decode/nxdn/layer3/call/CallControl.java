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
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer2.LICH;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNLayer3Message;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallControlOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallType;

/**
 * Base call control message
 */
public abstract class CallControl extends NXDNLayer3Message
{
    private static final IntField CC_OPTION = IntField.length8(OCTET_1);
    private static final IntField CALL_TYPE = IntField.length3(OCTET_2);
    private CallControlOption mCallControlOption;
    protected NXDNRadioIdentifier mSourceIdentifier;
    protected IntegerIdentifier mDestinationIdentifier;

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     * @param type of message
     * @param ran value
     * @param lich info
     */
    public CallControl(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type, int ran, LICH lich)
    {
        super(message, timestamp, type, ran, lich);
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
     * Call type for this voice call.
     * @return type
     */
    public CallType getCallType()
    {
        return CallType.fromValue(getMessage().getInt(CALL_TYPE));
    }

    /**
     * Source radio ID
     * @return source identifier.
     */
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null)
        {
            mSourceIdentifier = NXDNRadioIdentifier.createFrom(getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return mSourceIdentifier;
    }

    /**
     * Destination identifier, either talkgroup or radio.
     * @return destination identifier
     */
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null)
        {
            mDestinationIdentifier = switch (getCallType())
            {
                case GROUP_BROADCAST, GROUP_CONFERENCE -> NXDNTalkgroupIdentifier.createTo(getMessage().getInt(IDENTIFIER_OCTET_5));
                default -> NXDNRadioIdentifier.createTo(getMessage().getInt(IDENTIFIER_OCTET_5));
            };
        }

        return mDestinationIdentifier;
    }
}
