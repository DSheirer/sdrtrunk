/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.talkgroup.TalkgroupIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25AnyTalkgroup;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacStructure;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Telephone interconnect voice channel user
 */
public class TelephoneInterconnectVoiceChannelUser extends MacStructure
{
    private static final int[] SERVICE_OPTIONS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] CALL_TIMER = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] SOURCE_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
        49, 50, 51, 52, 53, 54, 55};

    private List<Identifier> mIdentifiers;
    private TalkgroupIdentifier mToOrFromAddress;
    private VoiceServiceOptions mServiceOptions;

    /**
     * Constructs the message
     *
     * @param message containing the message bits
     * @param offset into the message for this structure
     */
    public TelephoneInterconnectVoiceChannelUser(CorrectedBinaryMessage message, int offset)
    {
        super(message, offset);
    }

    /**
     * Textual representation of this message
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getOpcode());
        sb.append(" TO/FROM:").append(getToOrFromAddress());

        long timer = getCallTimer();

        if(timer == 0)
        {
            sb.append(" TIMER:none");
        }
        else
        {
            sb.append(" TIMER:").append(timer / 1000d).append("seconds");
        }

        sb.append(" ").append(getServiceOptions());

        return sb.toString();
    }

    /**
     * Voice channel service options
     */
    public VoiceServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS, getOffset()));
        }

        return mServiceOptions;
    }

    /**
     * Call timer in milliseconds.
     *
     * @return timer in milliseconds where a value of 0 indicates no timer.
     */
    public long getCallTimer()
    {
        return getMessage().getInt(CALL_TIMER, getOffset()) * 100; //milliseconds
    }

    /**
     * From Radio Unit
     */
    public TalkgroupIdentifier getToOrFromAddress()
    {
        if(mToOrFromAddress == null)
        {
            mToOrFromAddress = APCO25AnyTalkgroup.create(getMessage().getInt(SOURCE_ADDRESS, getOffset()));
        }

        return mToOrFromAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getToOrFromAddress());
        }

        return mIdentifiers;
    }
}
