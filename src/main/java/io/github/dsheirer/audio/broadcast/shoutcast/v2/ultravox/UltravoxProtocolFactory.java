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

import org.apache.mina.core.session.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFactory;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;

public class UltravoxProtocolFactory implements ProtocolCodecFactory
{
    private ProtocolEncoder mEncoder;
    private ProtocolDecoder mDecoder;

    @Override
    public ProtocolEncoder getEncoder(IoSession ioSession) throws Exception
    {
        if(mEncoder == null)
        {
            mEncoder = new UltravoxEncoder();
        }

        return mEncoder;
    }

    @Override
    public ProtocolDecoder getDecoder(IoSession ioSession) throws Exception
    {
        if(mDecoder == null)
        {
            mDecoder = new UltravoxDecoder();
        }

        return mDecoder;
    }
}
