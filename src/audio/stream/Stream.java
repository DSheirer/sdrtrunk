/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2016 Dennis Sheirer
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
 *
 ******************************************************************************/
package audio.stream;

import audio.AudioPacket;
import audio.stream.shout.channel.IShoutChannel;
import audio.stream.shout.server.IShoutServer;
import sample.Listener;

/**
 * Streaming channel encapsulates communication with a specific server and channel.
 */
public class Stream implements Listener<AudioPacket>
{
    private IShoutServer mServer;
    private IShoutChannel mChannel;

    public Stream(IShoutServer server, IShoutChannel channel)
    {
        mServer = server;
        mChannel = channel;
    }

    public IShoutServer getServer()
    {
        return mServer;
    }

    public IShoutChannel getChannel()
    {
        return mChannel;
    }

    @Override
    public void receive(AudioPacket audioPacket)
    {

    }
}
