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

    /**
     * Constructs a CTCSS message
     * @param code detected CTCSS code
     * @param timestamp of detection
     */
    public CTCSSMessage(CTCSSCode code, long timestamp)
    {
        super(timestamp);
        mCTCSSCode = code;
        mIdentifier = new CTCSSIdentifier(code);
    }

    /**
     * @return the detected CTCSS code
     */
    public CTCSSCode getCTCSSCode()
    {
        return mCTCSSCode;
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
        return Collections.singletonList(mIdentifier);
    }

    @Override
    public String toString()
    {
        return "CTCSS: " + mCTCSSCode.getDisplayString();
    }
}