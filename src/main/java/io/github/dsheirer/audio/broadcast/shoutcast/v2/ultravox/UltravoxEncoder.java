/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.shoutcast.v2.ultravox;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;

public class UltravoxEncoder extends ProtocolEncoderAdapter
{
    @Override
    public void encode(IoSession ioSession, Object message, ProtocolEncoderOutput out) throws Exception
    {
        if(message instanceof UltravoxMessage)
        {
            byte[] data = ((UltravoxMessage)message).getMessage();

            IoBuffer buf = IoBuffer.allocate(data.length).setAutoExpand(false);
            buf.put(data);
            buf.flip();
            out.write(buf);
        }
    }
}
