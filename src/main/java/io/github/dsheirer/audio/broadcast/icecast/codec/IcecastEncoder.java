/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.audio.broadcast.icecast.codec;

import org.apache.mina.core.buffer.IoBuffer;
import org.apache.mina.core.session.AttributeKey;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolEncoderAdapter;
import org.apache.mina.filter.codec.ProtocolEncoderOutput;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineEncoder;

import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.charset.StandardCharsets;

public class IcecastEncoder extends ProtocolEncoderAdapter
{
    private static final AttributeKey ENCODER = new AttributeKey(TextLineEncoder.class, "encoder");
    private static final Charset mCharset = StandardCharsets.UTF_8;
    private static final int MAX_LINE_LENGTH = 2147483647;

    /**
     * Icecast Protocol Encoder is an Apache Mina compliant protocol encoder that supports Icecast string-based
     * connection negotiation as well as byte array audio frame data.
     */
    public IcecastEncoder()
    {
    }

    @Override
    public void encode(IoSession session, Object message, ProtocolEncoderOutput out) throws Exception
    {
        //Audio frame data
        if(message instanceof byte[])
        {
            byte[] data = (byte[])message;

            IoBuffer buf = IoBuffer.allocate(data.length).setAutoExpand(false);
            buf.put(data);
            buf.flip();
            out.write(buf);
        }
        //Icecast connection messages
        else if(message instanceof String)
        {
            CharsetEncoder encoder = (CharsetEncoder)session.getAttribute(ENCODER);

            if(encoder == null)
            {
                encoder = mCharset.newEncoder();
                session.setAttribute(ENCODER, encoder);
            }

            String value = (String)message;

            IoBuffer buf = IoBuffer.allocate(value.length()).setAutoExpand(true);

            buf.putString(value, encoder);


            if(buf.position() > MAX_LINE_LENGTH)
            {
                throw new IllegalArgumentException("Line length: " + buf.position());
            }
            else
            {
                buf.putString(LineDelimiter.UNIX.getValue(), encoder);
                buf.flip();
                out.write(buf);
            }
        }
        else
        {
            throw new IllegalArgumentException("Can't encode - unrecognized object type: " + message.getClass());
        }
    }
}
