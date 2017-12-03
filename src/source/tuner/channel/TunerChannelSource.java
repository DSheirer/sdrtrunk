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
package source.tuner.channel;

import dsp.filter.FilterFactory;
import dsp.filter.Window.WindowType;
import dsp.filter.cic.ComplexPrimeCICDecimate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Buffer;
import sample.Listener;
import sample.complex.Complex;
import sample.complex.ComplexBuffer;
import sample.real.IOverflowListener;
import source.ComplexSource;
import source.SourceException;
import source.ISourceEventListener;
import source.ISourceEventProvider;
import source.SourceEvent;
import source.ISourceEventProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public abstract class TunerChannelSource extends ComplexSource implements ISourceEventProcessor, Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerChannelSource.class);
    private TunerChannel mTunerChannel;
    protected Listener<SourceEvent> mSourceEventListener;

    /**
     * Requirements:
     * 1. Provide complex buffer samples
     * 2. Fetch complex buffer samples from subclass implementations (for distribution)
     * 3. Provide heartbeat at each interval
     * 4. Provide start/stop running
     * 5. Provide support for downstream consumer source events (delegate to the sub class)
     * 6. Support buffer overflow listeners
     *
     * @param sourceEventListener to receive source dispose request
     * @param tunerChannel
     */
    public TunerChannelSource(Listener<SourceEvent> sourceEventListener, TunerChannel tunerChannel)
    {
        mSourceEventListener = sourceEventListener;
    }

    /**
     * Tuner channel for this tuner channel source
     */
    public TunerChannel getTunerChannel()
    {
        return mTunerChannel;
    }

    /**
     * Starts this tuner channel source producing sample stream.
     */
    public void start()
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(SourceEvent.startSampleStream(this));
        }

        //TODO: broadcast center frequency and sample rate to downstream listeners.
    }

    /**
     * Stops this tuner channel source from producing a sample stream.  Note: tuner channel sources are one-time usage
     * only.  Invoking this method will also tell the source manager to dispose of this source.
     */
    public void stop()
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(SourceEvent.stopSampleStream(this));
            mSourceEventListener.receive(SourceEvent.sourceDisposeRequest(this));
        }
    }

    @Override
    public void reset()
    {
        //Reset is not valid for a tuner channel source - ignored
    }
}
