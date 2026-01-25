/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.squelch.ISquelchStateListener;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.real.IRealFilter;
import io.github.dsheirer.dsp.filter.fir.remez.RemezFIRFilterDesigner;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.real.IRealBufferListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides packaging of demodulated audio sample buffers into audio segments for broadcast to registered listeners.
 * Includes audio packet metadata in constructed audio segments.
 *
 * Incorporates audio squelch state listener to control if audio packets are broadcast or ignored.
 */
public class AudioModule extends AbstractAudioModule implements ISquelchStateListener, IRealBufferListener,
    Listener<float[]>
{
    private static final Logger mLog = LoggerFactory.getLogger(AudioModule.class);
    private static float[] sHighPassFilterCoefficients;
    private final boolean mAudioFilterEnable;
    private final int mSquelchDelayTimeMs;
    private final boolean mSquelchDelayRemoveSilence;
    private ScheduledExecutorService mDelayExecutor;
    private ScheduledFuture<?> mPendingClose;
    private boolean mInDelayPeriod = false;

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

    private final IRealFilter mHighPassFilter = FilterFactory.getRealFilter(sHighPassFilterCoefficients);
    private final SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private SquelchState mSquelchState = SquelchState.SQUELCH;

    /**
     * Creates an Audio Module.
     *
     * @param aliasList for aliasing identifiers
     * @param timeslot for this audio module
     * @param maxAudioSegmentLength in milliseconds
     * @param audioFilterEnable to enable or disable high-pass audio filter
     * @param squelchDelayTimeMs delay time before closing audio segment after squelch closes
     * @param squelchDelayRemoveSilence true to remove silence during delay, false to preserve it
     */
    public AudioModule(AliasList aliasList, int timeslot, long maxAudioSegmentLength, boolean audioFilterEnable, int squelchDelayTimeMs, boolean squelchDelayRemoveSilence)
    {
        super(aliasList, timeslot, maxAudioSegmentLength);
        mAudioFilterEnable = audioFilterEnable;
        mSquelchDelayTimeMs = squelchDelayTimeMs;
        mSquelchDelayRemoveSilence = squelchDelayRemoveSilence;

        if(mSquelchDelayTimeMs > 0)
        {
            mDelayExecutor = Executors.newSingleThreadScheduledExecutor();
        }
    }

    /**
     * Creates an Audio Module.
     * @param aliasList for aliasing identifiers
     * @param audioFilterEnable to enable or disable high-pass audio filter
     */
    public AudioModule(AliasList aliasList, boolean audioFilterEnable)
    {
        super(aliasList);
        mAudioFilterEnable = audioFilterEnable;
        mSquelchDelayTimeMs = 0;
        mSquelchDelayRemoveSilence = true;
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
    public void stop()
    {
        // Cancel any pending close
        if(mPendingClose != null && !mPendingClose.isDone())
        {
            mPendingClose.cancel(false);
        }

        // Shutdown the executor
        if(mDelayExecutor != null)
        {
            mDelayExecutor.shutdownNow();
        }

        super.stop();
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    @Override
    public void receive(float[] audioBuffer)
    {
        if(mSquelchState == SquelchState.UNSQUELCH)
        {
            if(mAudioFilterEnable)
            {
                audioBuffer = mHighPassFilter.filter(audioBuffer);
            }

            addAudio(audioBuffer);
        }
        else if(mInDelayPeriod && !mSquelchDelayRemoveSilence)
        {
            // During delay period, add silence to preserve timing (if not removing silence)
            float[] silence = new float[audioBuffer.length];
            addAudio(silence);
        }
    }

    @Override
    public Listener<float[]> getBufferListener()
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
            SquelchState squelchState = event.getSquelchState();

            if(mSquelchState != squelchState)
            {
                mSquelchState = squelchState;

                if(mSquelchState == SquelchState.SQUELCH)
                {
                    // If delay is configured, schedule the close; otherwise close immediately
                    if(mSquelchDelayTimeMs > 0 && mDelayExecutor != null)
                    {
                        mInDelayPeriod = true;

                        // Cancel any existing pending close
                        if(mPendingClose != null && !mPendingClose.isDone())
                        {
                            mPendingClose.cancel(false);
                        }

                        // Schedule delayed close
                        mPendingClose = mDelayExecutor.schedule(() -> {
                            // Only close if still squelched
                            if(mSquelchState == SquelchState.SQUELCH)
                            {
                                mInDelayPeriod = false;
                                closeAudioSegment();
                            }
                        }, mSquelchDelayTimeMs, TimeUnit.MILLISECONDS);
                    }
                    else
                    {
                        closeAudioSegment();
                    }
                }
                else
                {
                    // Squelch opened - cancel any pending close
                    mInDelayPeriod = false;
                    if(mPendingClose != null && !mPendingClose.isDone())
                    {
                        mPendingClose.cancel(false);
                    }
                }
            }
        }
    }
}