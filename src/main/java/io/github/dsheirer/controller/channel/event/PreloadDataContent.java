/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.controller.channel.event;

import io.github.dsheirer.module.ModuleEventBusMessage;

/**
 * Data that should be preloaded into a processing chain by broadcasting the data onto the processing chain so that
 * any module can subscribe to it.
 * @param <T> data generic that should be loaded into modules in the processing chain.
 */
public abstract class PreloadDataContent<T> extends ModuleEventBusMessage
{
    private final T mData;

    /**
     * Constructs an instance
     * @param data to preload
     */
    public PreloadDataContent(T data)
    {
        mData = data;
    }

    /**
     * Data to preload
     * @return
     */
    public T getData()
    {
        return mData;
    }

    /**
     * Indicates if the data payload is non-null
     */
    public boolean hasData()
    {
        return mData != null;
    }
}
