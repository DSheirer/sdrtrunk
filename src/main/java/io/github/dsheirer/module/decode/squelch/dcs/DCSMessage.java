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

package io.github.dsheirer.module.decode.squelch.dcs;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.dcs.DCSIdentifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.squelch.SquelchCodeState;
import io.github.dsheirer.protocol.Protocol;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * Digital Coded Squelch (DCS) tone detected message
 */
public class DCSMessage extends Message
{
    // null is a necessary default for many of these
    private DCSCode mDCSCode = null;
    private DCSCode mConfiguredCode = null;
    private String mDebugMessage = null;
    private DCSIdentifier mIdentifier = null;
    private boolean mMutedStatus = true;
    private DecoderStateEvent.Event mCallEvent = null;
    private SquelchCodeState mCodeState = null;

    /**
     * Constructs an instance
     * @param code that was detected
     * @param timestamp when the code was detected
     */
    public DCSMessage(DCSCode code, long timestamp)
    {
        super();
        mDCSCode = code;
    }

    /**
     * Constructs an instance
     * @param configuredCode that was detected
     */
    public DCSMessage(DCSCode configuredCode)
    {
        super();    // takes care of timestamp
        mConfiguredCode = configuredCode;
    }

    /**
     * Constructs an instance, usually at squelch close
     * @param configuredCode that was detected
     * @param initialMessage for debug or log
     * @param callEvent for the initial DecoderStateEvent (usually END)
     * @param codeState for the initial code state (usually LOST)
     */
    public DCSMessage(DCSCode configuredCode, String initialMessage, DecoderStateEvent.Event callEvent, SquelchCodeState codeState)
    {
        super();    // takes care of timestamp
        mConfiguredCode = configuredCode;
        mDebugMessage = initialMessage;
        mCallEvent = callEvent;
        mCodeState = codeState;
    }

    /**
     * Constructs an instance
     */
    public DCSMessage()
    {
        super();
    }

    @Override
    public String toString()
    {
        String DCSString = mDCSCode != null ? mDCSCode.toString() : "None";
        String mutedString = mMutedStatus ? "MUTED" : "UNMUTED";
        String message = mDebugMessage != null ? mDebugMessage : "";

        String s = MessageFormat.format("DCS: {0}, Config: {1}, Detected: {2}, {3}",
                mutedString,
                mConfiguredCode,
                DCSString,
                message
        );
        return s;
    }

    /**
     * The DCS code that was detected.
     * @return code
     */
    public DCSCode getDCSCode()
    {
        return mDCSCode;
    }

    @Override
    public boolean isValid()
    {
        return true; //We only send a message when the tone was valid
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.DCS;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifier != null)
            return Collections.singletonList(mIdentifier);
        else
            return Collections.emptyList();
    }

    public void setDCSCode(DCSCode code)
    {
        mDCSCode = code;
        mIdentifier = code != null ? new DCSIdentifier(code) : null;
    }

    public void setMessage(String s)
    {
        mDebugMessage = s;
    }

    public void setMutedStatus(boolean b)
    {
        mMutedStatus = b;
    }

    public boolean getMutedStatus()
    {
        return mMutedStatus;
    }

    public void setCodeState(SquelchCodeState state)
    {
        mCodeState = state;
    }

    public SquelchCodeState getCodeState()
    {
        return mCodeState;
    }

    public void setCallEvent(DecoderStateEvent.Event state)
    {
        mCallEvent = state;
    }

    public DecoderStateEvent.Event getCallEvent()
    {
        return mCallEvent;
    }

}
