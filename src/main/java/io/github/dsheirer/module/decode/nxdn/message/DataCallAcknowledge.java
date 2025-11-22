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

package io.github.dsheirer.module.decode.nxdn.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNTalkgroupIdentifier;
import io.github.dsheirer.module.decode.nxdn.type.CallControlOption;
import io.github.dsheirer.module.decode.nxdn.type.CallType;
import io.github.dsheirer.module.decode.nxdn.type.DataCallOption;
import io.github.dsheirer.module.decode.nxdn.type.ErrorBlockFlags;
import io.github.dsheirer.module.decode.nxdn.type.ResponseInformation;
import java.util.List;

/**
 * Response to confirmed data call
 */
public class DataCallAcknowledge extends NXDNMessage
{
    private static final IntField CC_OPTION = IntField.length8(OCTET_1);
    private static final IntField CALL_TYPE = IntField.length3(OCTET_2);
    private static final IntField CALL_OPTION = IntField.length5(OCTET_2 + 3);
    private static final IntField RESPONSE_INFORMATION = IntField.length16(OCTET_7);
    private static final IntField ERROR_BLOCK_FLAG = IntField.length16(OCTET_9);

    private DataCallOption mDataCallOption;
    private CallControlOption mCallControlOption;
    private NXDNRadioIdentifier mSourceIdentifier;
    private IntegerIdentifier mDestinationIdentifier;
    private ResponseInformation mResponseInformation;
    private ErrorBlockFlags mErrorBlockFlags;

    /**
     * Constructs an instance
     *
     * @param message with binary data
     * @param timestamp for the message
     */
    public DataCallAcknowledge(CorrectedBinaryMessage message, long timestamp)
    {
        super(message, timestamp);
    }

    @Override
    public NXDNMessageType getMessageType()
    {
        return NXDNMessageType.TC_DATA_CALL_ACKNOWLEDGE;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("DATA CALL ACKNOWLEDGE");
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getResponseInformation());
        sb.append(" ").append(getErrorBlockFlags());
        return sb.toString();
    }

    /**
     * Data call options
     */
    public DataCallOption getCallOption()
    {
        if(mDataCallOption == null)
        {
            mDataCallOption = new DataCallOption(getMessage().getInt(CALL_OPTION));
        }

        return mDataCallOption;
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
            mSourceIdentifier = NXDNRadioIdentifier.from(getMessage().getInt(SOURCE));
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
                case GROUP_BROADCAST, GROUP_CONFERENCE -> NXDNTalkgroupIdentifier.to(getMessage().getInt(DESTINATION));
                default -> NXDNRadioIdentifier.to(getMessage().getInt(DESTINATION));
            };
        }

        return mDestinationIdentifier;
    }

    /**
     * Response with acknowledge or no-acknowledge.
     * @return response information.
     */
    public ResponseInformation getResponseInformation()
    {
        if(mResponseInformation == null)
        {
            mResponseInformation = new ResponseInformation(getMessage().getInt(RESPONSE_INFORMATION));
        }

        return mResponseInformation;
    }

    /**
     * Flags indicating error state with any received blocks.
     * @return flags
     */
    public ErrorBlockFlags getErrorBlockFlags()
    {
        if(mErrorBlockFlags == null)
        {
            mErrorBlockFlags = new ErrorBlockFlags(getMessage().getInt(ERROR_BLOCK_FLAG));
        }

        return mErrorBlockFlags;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return List.of(getSource(), getDestination());
    }
}
