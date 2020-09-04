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

package io.github.dsheirer.module.decode;

import io.github.dsheirer.audio.squelch.ISquelchStateProvider;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.sample.Listener;

/**
 * Primary decoder adds the following functionality over the basic decoder:
 *
 * - Provides audio squelch control
 */

public abstract class PrimaryDecoder extends Decoder implements ISquelchStateProvider
{
    protected Listener<SquelchStateEvent> mSquelchStateListener;

    protected DecodeConfiguration mDecodeConfiguration;

    public PrimaryDecoder(DecodeConfiguration config)
    {
        mDecodeConfiguration = config;
    }

    public DecodeConfiguration getDecodeConfiguration()
    {
        return mDecodeConfiguration;
    }

    public void broadcast(SquelchStateEvent state)
    {
        if(mSquelchStateListener != null)
        {
            mSquelchStateListener.receive(state);
        }
    }

    @Override
    public void setSquelchStateListener(Listener<SquelchStateEvent> listener)
    {
        mSquelchStateListener = listener;
    }

    @Override
    public void removeSquelchStateListener()
    {
        mSquelchStateListener = null;
    }
}
