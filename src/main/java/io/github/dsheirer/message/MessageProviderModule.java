/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.message;

import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;

/**
 * Testing module to use for injecting IMessages into a processing chain
 */
public class MessageProviderModule extends Module implements IMessageProvider
{
    private Listener<IMessage> mMessageListener;

    public MessageProviderModule()
    {

    }

    @Override
    public void reset()
    {

    }

    @Override
    public void start()
    {

    }

    @Override
    public void stop()
    {

    }

    public void receive(IMessage message)
    {
        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    @Override
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    @Override
    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
