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

package io.github.dsheirer.module.decode.nxdn.layer3;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CallControlOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseSS;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ControlCommand;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDPartial;
import java.util.List;

/**
 * SU radio response to trunking controller request for remote control of the SU
 */
public class RemoteControlResponseWithESN extends NXDNLayer3Message
{
    private static final IntField CC_OPTION = IntField.length8(OCTET_1);
    private static final IntField CONTROL_COMMAND_2 = IntField.length5(OCTET_2 + 3);
    private static final IntField CAUSE = IntField.length8(OCTET_6);
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_7);
    private static final int OFFSET_LOCATION_ID_PARTIAL = OCTET_7 + 5;
    private CallControlOption mCallControlOption;
    private NXDNRadioIdentifier mSourceIdentifier;
    private LocationIDPartial mLocationIDPartial;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public RemoteControlResponseWithESN(CorrectedBinaryMessage message, long timestamp)
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

        sb.append("REMOTE CONTROL WITH ESN ").append(getCommand()).append(" RESPONSE:").append(getCause());
        sb.append(" FROM:").append(getSource());

        if(getCallControlOption().hasLocationId())
        {
            sb.append(" ").append(getLocationIDOption()).append(" LOCATION:").append(getLocationIDOption());
        }

        return sb.toString();
    }

    /**
     * Response cause.
     */
    public CauseSS getCause()
    {
        return CauseSS.fromValue(getMessage().getInt(CAUSE));
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
     * Source radio identifier.
     * @return source
     */
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null)
        {
            mSourceIdentifier = NXDNRadioIdentifier.to(getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return mSourceIdentifier;
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
    public LocationIDPartial getLocationIDPartial()
    {
        if(mLocationIDPartial == null)
        {
            mLocationIDPartial = new LocationIDPartial(getMessage(), OFFSET_LOCATION_ID_PARTIAL);
        }

        return mLocationIDPartial;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource());
    }
}
