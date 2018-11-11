/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.module.decode.p25.message.lc.standard;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25AnyTalkgroup;
import io.github.dsheirer.module.decode.p25.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Current user of an APCO25 channel on both inbound and outbound channels
 */
public class LCTelephoneInterconnectVoiceChannelUser extends LinkControlWord
{
    private static final int[] RESERVED_1 = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] RESERVED_2 = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] CALL_TIMER = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    private ServiceOptions mServiceOptions;
    private Identifier mAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public LCTelephoneInterconnectVoiceChannelUser(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ID:").append(getAddress());
        sb.append(" CALL TIMER:").append(getCallTimerDuration()).append("MS");
        sb.append(" ").append(getServiceOptions());

        return sb.toString();
    }

    /**
     * Service Options for this channel
     */
    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Call timer duration in milliseconds.
     */
    public long getCallTimerDuration()
    {
        //Convert from 100 millisecond intervals to milliseconds.
        return getMessage().getInt(CALL_TIMER) * 100;
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public Identifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25AnyTalkgroup.create(getMessage().getInt(ADDRESS));
        }

        return mAddress;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getAddress());
        }

        return mIdentifiers;
    }
}
