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

package io.github.dsheirer.module.decode.edacs.provoice;

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;
import java.util.Collections;
import java.util.List;

/**
 * One ProVoice frame (80 ms) as four P25 IMBE voice frames for JMBE.  A null frame is a
 * damaged voice frame that is played as 20 ms of silence.
 */
public class ProVoiceMessage implements IMessage
{
    private final List<byte[]> mIMBEFrames;
    private final int mErrors;
    private final int mConcealed;
    private final long mTimestamp;

    public ProVoiceMessage(List<byte[]> imbeFrames, int errors, int concealed, long timestamp)
    {
        mIMBEFrames = imbeFrames;
        mErrors = errors;
        mConcealed = concealed;
        mTimestamp = timestamp;
    }

    public List<byte[]> getIMBEFrames()
    {
        return mIMBEFrames;
    }

    /**
     * Bit errors corrected in the four voice frames
     */
    public int getErrors()
    {
        return mErrors;
    }

    /**
     * Voice frames of the four that were damaged and replaced (repeat or silence)
     */
    public int getConcealed()
    {
        return mConcealed;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public boolean isValid()
    {
        return true;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.EDACS;
    }

    @Override
    public int getTimeslot()
    {
        return 0;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }

    @Override
    public String toString()
    {
        return "PROVOICE VOICE FRAME ERRORS:" + mErrors;
    }
}
