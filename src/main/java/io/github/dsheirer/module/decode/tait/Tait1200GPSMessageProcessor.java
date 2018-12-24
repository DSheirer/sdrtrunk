/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.module.decode.tait;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Tait1200GPSMessageProcessor implements Listener<CorrectedBinaryMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(Tait1200GPSMessageProcessor.class);

    private Listener<IMessage> mMessageListener;

    public Tait1200GPSMessageProcessor()
    {
    }

    public void dispose()
    {
        mMessageListener = null;
    }

    @Override
    public void receive(CorrectedBinaryMessage buffer)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(new Tait1200GPSMessage(buffer));
        }
    }

    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
