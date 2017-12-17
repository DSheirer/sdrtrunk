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
package io.github.dsheirer.audio.broadcast.icecast.codec;

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filter.codec.textline.LineDelimiter;
import org.apache.mina.filter.codec.textline.TextLineDecoder;

import java.nio.charset.StandardCharsets;

public class IcecastCodecFactory implements ProtocolCodecFactory
{
    private ProtocolEncoder mProtocolEncoder;
    private ProtocolDecoder mProtocolDecoder;

    /**
     * Factory for codecs to process Icecast string protocol and byte array audio frames for use with Apache Mina
     */
    public IcecastCodecFactory()
    {
    }

    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception
    {
        if(mProtocolEncoder == null)
        {
            mProtocolEncoder = new IcecastEncoder();
        }

        return mProtocolEncoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception
    {
        if(mProtocolDecoder == null)
        {
            mProtocolDecoder =  new TextLineDecoder(StandardCharsets.UTF_8, LineDelimiter.AUTO);
        }

        return mProtocolDecoder;
    }
}
