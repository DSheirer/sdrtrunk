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
package io.github.dsheirer.source;

import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceEventListenerToProcessorAdapter implements Listener<SourceEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(SourceEventListenerToProcessorAdapter.class);

    private ISourceEventProcessor mSourceEventProcessor;

    /**
     * Adapter to implement ISourceEventListener/Listener<SourceEvent> and adapt to a source event processor
     */
    public SourceEventListenerToProcessorAdapter(ISourceEventProcessor sourceEventProcessor)
    {
        mSourceEventProcessor = sourceEventProcessor;
    }

    public void dispose()
    {
        mSourceEventProcessor = null;
    }

    @Override
    public void receive(SourceEvent sourceEvent)
    {
        if(mSourceEventProcessor != null)
        {
            try
            {
                mSourceEventProcessor.process(sourceEvent);
            }
            catch(SourceException e)
            {
                mLog.error("Error while transferring source event to source event processor [" +
                    mSourceEventProcessor.getClass() + "]", e);
            }
        }
    }
}
