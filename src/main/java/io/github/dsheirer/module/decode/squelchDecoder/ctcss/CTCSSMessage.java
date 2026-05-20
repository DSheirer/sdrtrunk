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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.ctcss.CTCSSIdentifier;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.protocol.Protocol;

import java.util.Collections;
import java.util.List;

/**
 * CTCSS tone debug message
 */
public class CTCSSMessage extends Message
{
    private CTCSSCode mCTCSSCode;
    private String mDebugMessage;
    private CTCSSIdentifier mIdentifier;
    private boolean mInitialThreshold;

    /**
     * Constructs an instance
     */
    public CTCSSMessage()
    {
        super();            // takes care of timestamp
        mCTCSSCode = null;
        mDebugMessage = null;
        mInitialThreshold = false;
    }
    public CTCSSMessage(CTCSSCode code)
    {
        super();            // takes care of timestamp
        mCTCSSCode = code;
        mIdentifier = code != null ? new CTCSSIdentifier(code) : null;
        mDebugMessage = null;
    }

    @Override
    public String toString()
    {
        return mDebugMessage;
    }

    @Override
    public boolean isValid()
    {
        return mInitialThreshold;
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

    public void setInitialThreshold(boolean firstThreshold)
    {
        mInitialThreshold = firstThreshold;
    }


    public void setMessage(String s)
    {
        mDebugMessage = s;
    }

    public void setMessage(String message, String code, String format, String format1, String format2)
    {

    }
}
