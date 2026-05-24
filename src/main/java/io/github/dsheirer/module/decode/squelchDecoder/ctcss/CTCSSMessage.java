/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.squelchDecoder.ctcss;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.ctcss.CTCSSIdentifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;

/**
 * CTCSS Detector message carrying CTCSS code, state and debug info for distribution
 */
public class CTCSSMessage extends Message
{
    private CTCSSCode mConfiguredCode = null;
    private CTCSSCode mCTCSSCode = null;
    private String mDebugMessage = null;
    private CTCSSIdentifier mIdentifier = null;
    private boolean mMutedStatus = true;
    private boolean mFirstThreshold = false;
    private double mPower;
    private double mPowerThreshold;
    private DecoderStateEvent.Event mCallEvent;
    private SquelchCodeState mCodeState;

    public enum SquelchCodeState
    {
        ACCEPTED,
        REJECTED,
        LOST
    }

    /**
     * Constructs an instance
     */
    public CTCSSMessage()
    {
        super();            // takes care of timestamp
    }

    /**
     * constructor
     * @param configuredCode Configured code for unmuting audio
     */
    public CTCSSMessage(CTCSSCode configuredCode)
    {
        super();            // takes care of timestamp
        mConfiguredCode = configuredCode;
    }

    @Override
    public String toString()
    {
        String toneString = mCTCSSCode != null ? mCTCSSCode.toString() : "None";
        String mutedString = mMutedStatus ? "MUTED" : "UNMUTED";
        String message = mDebugMessage != null ? mDebugMessage : "";

        String s = MessageFormat.format("CTCSS: {0}, Config: {1}, Detected: {2}, 1st Thresh: {3}, Pwr: {4}, Pwr Thresh: {5}, {6}",
                mutedString,
                mConfiguredCode,
                toneString,
                mFirstThreshold,
                String.format("%.1f", mPower),
                String.format("%.1f", mPowerThreshold),
                message
        );
        return s;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.CTCSS;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifier != null)
        {
            return Collections.singletonList(mIdentifier);
        }
        return Collections.emptyList();
    }

    public void setCTCSSCode(CTCSSCode code)
    {
        mCTCSSCode = code;
        mIdentifier = code != null ? new CTCSSIdentifier(code) : null;
    }

    public CTCSSCode getCTCSSCode()
    {
        return mCTCSSCode;
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

    public void setFirstThreshold(boolean b)
    {
        mFirstThreshold = b;
    }

    public void setPower(double power)
    {
        mPower = power;
    }

    public void setPowerThreshold(double powerThreshold)
    {
        mPowerThreshold = powerThreshold;
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
