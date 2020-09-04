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

package io.github.dsheirer.message;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.module.HistoryModule;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decode event history module.  Maintains a history of decode events and constrains the total history size.
 */
public class MessageHistory extends HistoryModule<IMessage> implements IMessageListener
{
    private final static Logger mLog = LoggerFactory.getLogger(MessageHistory.class);

    /**
     * Constructs an instance
     */
    public MessageHistory(int historySize)
    {
        super(historySize);
    }

    /**
     * Implements the IDecodeEventListener interface - delegates to receive(event) method.
     */
    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }

    /**
     * Process a request for message history and post the response to the module event bus
     */
    @Subscribe
    public void process(MessageHistoryRequest request)
    {
        getInterModuleEventBus().post(new MessageHistoryResponse(getItems()));
    }

    /**
     * Processes a request to preload message history
     */
    @Subscribe
    public void process(MessageHistoryPreloadData preloadData)
    {
        for(IMessage message: preloadData.getData())
        {
            receive(message);
        }
    }
}
