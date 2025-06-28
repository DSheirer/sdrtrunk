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

package io.github.dsheirer.alias.id.talkgroup;

import io.github.dsheirer.protocol.Protocol;

/**
 * Streaming talkgroup alias.  For streamed audio metadata, this value is used to replace the decoded talkgroup or TO
 * value with an alias value when there are talkgroup collisions resulting from streaming multiple channels to the
 * same stream.  This is common in P25 conventional repeater setups where each radio channel is using talkgroup 1.
 */
public class StreamAsTalkgroup extends Talkgroup
{
    public StreamAsTalkgroup(int talkgroup)
    {
        super(Protocol.UNKNOWN, talkgroup);
    }

    public StreamAsTalkgroup()
    {
        //No arg JAXB constructor
    }
}
