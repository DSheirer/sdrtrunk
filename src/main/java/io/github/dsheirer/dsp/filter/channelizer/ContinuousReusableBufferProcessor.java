/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.dsp.filter.channelizer;

import io.github.dsheirer.sample.buffer.AbstractBuffer;
import io.github.dsheirer.sample.buffer.OverflowableBufferTransferQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ContinuousReusableBufferProcessor<T extends AbstractBuffer> extends ContinuousBufferProcessor<T>
{
    private final static Logger mLog = LoggerFactory.getLogger(ContinuousReusableBufferProcessor.class);

    /**
     * Scheduled Reusable Buffer Processor combines an internal overflowable buffer with a scheduled runnable processing
     * task for periodically distributing internally queued elements to the registered listener.  This processor
     * provides a convenient way to create a thread-safe buffer for receiving elements from one thread/runnable and then
     * distributing those elements to a registered listener where distribution occurs on a separate scheduled thread
     * pool runnable thread.  This allows the calling input thread to quickly return without incurring any subsequent
     * processing workload.
     *
     * The internal queue is an overflowable queue implementation that allows a listener to be registered to receive
     * notifications of overflow and reset state.  Queue sizing parameters are specified in the constructor.
     *
     * @param maximumSize of the internal queue (overflow happens when this is exceeded)
     * @param resetThreshold of the internal queue (overflow reset happens once queue size falls below this threshold
     */
    public ContinuousReusableBufferProcessor(int maximumSize, int resetThreshold)
    {
        super(new OverflowableBufferTransferQueue<T>(maximumSize, resetThreshold));
    }

    /**
     * Distributes queued buffers to the listener
     */
    @Override
    protected void process()
    {
        List<T> buffers = new ArrayList<>();

        mQueue.drainTo(buffers);

        try
        {
            if(getListener() != null)
            {
                getListener().receive(buffers);
            }

            buffers.clear();
        }
        catch(Throwable throwable)
        {
            mLog.error("Error while dispatching buffers to listener.  Performing buffer user count cleanup", throwable);

            if(!buffers.isEmpty())
            {
                for(T buffer: buffers)
                {
                    try
                    {

                        }
                    catch(IllegalStateException ise)
                    {
                        mLog.error("Error while performing user count cleanup on reusable buffers.");
                    }
                }
            }
        }
    }
}
