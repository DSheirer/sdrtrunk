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

package io.github.dsheirer.module.decode.pager.pocsag;

import io.github.dsheirer.module.decode.pager.PagerMessage;
import io.github.dsheirer.module.decode.pager.PagerMessageType;
import io.github.dsheirer.protocol.Protocol;

/**
 * POCSAG paging message.
 */
public class PocsagMessage extends PagerMessage
{
    private final int mFunction;
    private final boolean mAfsk;

    /**
     * Constructs an instance
     * @param timestamp of the message
     * @param capcode (21-bit address)
     * @param function code (0-3) from the address codeword
     * @param messageType of the content
     * @param content decoded
     * @param bitRate 512, 1200 or 2400
     * @param errorCount of uncorrectable message codewords
     * @param afsk true if the page was sent as AFSK audio tones instead of direct FSK
     */
    public PocsagMessage(long timestamp, int capcode, int function, PagerMessageType messageType, String content,
                         int bitRate, int errorCount, boolean afsk)
    {
        super(timestamp, capcode, messageType, content, bitRate, errorCount);
        mFunction = function;
        mAfsk = afsk;
    }

    /**
     * Indicates if the page was sent as AFSK audio tones over a voice channel, instead of directly frequency shift
     * keying the carrier.
     */
    public boolean isAfsk()
    {
        return mAfsk;
    }

    /**
     * Function code (0-3) from the address codeword.  The meaning is assigned by the paging system, but commonly
     * function 0 is numeric and function 3 is alphanumeric.
     */
    public int getFunction()
    {
        return mFunction;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.POCSAG;
    }

    @Override
    protected String getProtocolDetails()
    {
        return (mAfsk ? "AFSK " : "") + "FUNCTION:" + mFunction;
    }
}
