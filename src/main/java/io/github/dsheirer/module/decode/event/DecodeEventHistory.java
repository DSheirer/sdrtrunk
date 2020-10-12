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

package io.github.dsheirer.module.decode.event;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.module.HistoryModule;
import io.github.dsheirer.sample.Listener;

/**
 * Decode event history module.  Maintains a history of decode events and constrains the total history size.
 */
public class DecodeEventHistory extends HistoryModule<IDecodeEvent> implements IDecodeEventListener
{
    /**
     * Constructs an instance
     */
    public DecodeEventHistory(int historySize)
    {
        super(historySize);
    }

    /**
     * Implements the IDecodeEventListener interface - delegates to receive(event) method.
     */
    @Override
    public Listener<IDecodeEvent> getDecodeEventListener()
    {
        return this;
    }

    /**
     * Process preload data
     */
    @Subscribe
    public void process(DecodeEventHistoryPreloadData preloadData)
    {
        for(IDecodeEvent decodeEvent: preloadData.getData())
        {
            receive(decodeEvent);
        }
    }

    /**
     * Processes a request for decode event history and posts the response back
     * to the processing chain event bus so that any of the modules can receive that history.
     *
     * Note: this is principally used by the DMR decoder for Capacity+ REST channel rotation to transfer the decode
     * event history from a traffic channel conversion to the new rest channel.
     *
     * @param request for decode event history
     */
    @Subscribe
    public void process(DecodeEventHistoryRequest request)
    {
        getInterModuleEventBus().post(new DecodeEventHistoryResponse(this));
    }
}
