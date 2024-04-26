/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase2.message.mac.structure;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25RadioIdentifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Telephone interconnect voice channel user
 */
public class TelephoneInterconnectVoiceChannelUser extends MacStructureVoiceService
{
    private static final IntField CALL_TIMER = IntField.range(16, 31);
    private static final IntField TARGET_ADDRESS = IntField.range(32, 55);
    private List<Identifier> mIdentifiers;
    private Identifier mTargetAddress;

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
        sb.append(" TO/FROM:").append(getTargetAddress());

        if(hasCallTimer())
        {
            sb.append(" TIMER:").append(getCallTimer() / 1000d).append("seconds");
        }
        else
        {
            sb.append(" TIMER:none");
        }

        sb.append(" ").append(getServiceOptions());

        return sb.toString();
    }

    /**
     * Call timer in milliseconds.
     *
     * @return timer in milliseconds where a value of 0 indicates no timer.
     */
    public long getCallTimer()
    {
        return getInt(CALL_TIMER) * 100; //milliseconds
    }

    /**
     * Indicates if this message has a non-zero call timer value.
     */
    public boolean hasCallTimer()
    {
        return hasInt(CALL_TIMER);
    }

    /**
     * The Radio Unit talking to a landline.  The role can be either TO or FROM, but we use the TO role for consistency.
     */
    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25RadioIdentifier.createFrom(getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
