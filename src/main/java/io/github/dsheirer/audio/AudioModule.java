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
package io.github.dsheirer.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.RealFIRFilter2;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferListener;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides packaging of demodulated audio sample buffers into audio segments for broadcast to registered listeners.
 * Includes audio packet metadata in constructed audio segments.
 *
 * Incorporates audio squelch state listener to control if audio packets are broadcast or ignored.
 */
public class AudioModule extends AbstractAudioModule implements ISquelchStateListener, IReusableBufferListener,
    Listener<ReusableFloatBuffer>
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);
    private static float[] sHighPassFilterCoefficients;

    static
    {
        FIRFilterSpecification specification = FIRFilterSpecification.highPassBuilder()
            .sampleRate(8000)
            .stopBandCutoff(200)
            .stopBandAmplitude(0.0)
            .stopBandRipple(0.025)
            .passBandStart(300)
            .passBandAmplitude(1.0)
            .passBandRipple(0.01)
            .build();
        try
        {
            RemezFIRFilterDesigner designer = new RemezFIRFilterDesigner(specification);

            if(designer.isValid())
            {
                sHighPassFilterCoefficients = designer.getImpulseResponse();
            }
        }
        catch(FilterDesignException fde)
        {
            mLog.error("Filter design error", fde);
        }
    }

    private RealFIRFilter2 mHighPassFilter = new RealFIRFilter2(sHighPassFilterCoefficients);
    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;

    /**
     * Creates an Audio Module.
     *
     * @param aliasList for aliasing identifiers
     * @param maxAudioSegmentLength in milliseconds
     */
    public AudioModule(AliasList aliasList, int timeslot, long maxAudioSegmentLength)
    {
        super(aliasList, timeslot, maxAudioSegmentLength);
    }

    /**
     * Creates an Audio Module.
     */
    public AudioModule(AliasList aliasList)
    {
        super(aliasList);
    }

    @Override
    protected int getTimeslot()
    {
        return 0;
    }

    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
    }

    @Override
    public void start()
    {
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void receive(ReusableFloatBuffer audioBuffer)
    {
        if(mSquelchState == SquelchState.UNSQUELCH)
        {
            ReusableFloatBuffer highPassFilteredAudio = mHighPassFilter.filter(audioBuffer);
            addAudio(highPassFilteredAudio.getSamplesCopy());
            highPassFilteredAudio.decrementUserCount();
        }
        else
        {
            audioBuffer.decrementUserCount();
        }
    }

    @Override
    public Listener<ReusableFloatBuffer> getReusableBufferListener()
    {
        //Redirect received reusable buffers to the receive(buffer) method
        return this;
    }

    /**
     * Wrapper for squelch state listener
     */
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            mSquelchState = event.getSquelchState();

            if(mSquelchState == SquelchState.SQUELCH)
            {
                closeAudioSegment();
            }
        }
    }
}
