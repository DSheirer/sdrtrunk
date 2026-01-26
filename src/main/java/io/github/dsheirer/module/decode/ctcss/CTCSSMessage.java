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
package io.github.dsheirer.module.decode.ctcss;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * CTCSS (Continuous Tone-Coded Squelch System) message representing a detected tone.
 */
public class CTCSSMessage extends Message
{
    private final CTCSSCode mCTCSSCode;
    private final CTCSSIdentifier mIdentifier;
    private final boolean mToneLost;

    /**
     * Constructs a CTCSS message for tone detection
     * @param code detected CTCSS code
     * @param timestamp of detection
     */
    public CTCSSMessage(CTCSSCode code, long timestamp)
    {
        this(code, timestamp, false);
    }

    /**
     * Constructs a CTCSS message
     * @param code CTCSS code
     * @param timestamp of event
     * @param toneLost true if this message indicates tone was lost
     */
    public CTCSSMessage(CTCSSCode code, long timestamp, boolean toneLost)
    {
        super(timestamp);
        mCTCSSCode = code;
        mIdentifier = code != null ? new CTCSSIdentifier(code) : null;
        mToneLost = toneLost;
    }

    /**
     * @return the detected CTCSS code
     */
    public CTCSSCode getCTCSSCode()
    {
        return mCTCSSCode;
    }

    /**
     * @return true if this message indicates tone was lost
     */
    public boolean isToneLost()
    {
        return mToneLost;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.CTCSS;
    }

    @Override
    public boolean isValid()
    {
        return mCTCSSCode != null && mCTCSSCode != CTCSSCode.UNKNOWN;
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

    @Override
    public String toString()
    {
        if(mToneLost)
        {
            return "CTCSS: Tone Lost";
        }
        return "CTCSS: " + mCTCSSCode.getDisplayString();
    }
}