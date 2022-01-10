/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.nbfm;

import io.github.dsheirer.channel.state.DecoderStateEvent;
import io.github.dsheirer.channel.state.IDecoderStateEventProvider;
import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.decimate.DecimationFilterFactory;
import io.github.dsheirer.dsp.filter.decimate.IComplexDecimationFilter;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.filter.fir.FIRFilterSpecification;
import io.github.dsheirer.dsp.filter.fir.complex.ComplexFIRFilter2;
import io.github.dsheirer.dsp.filter.resample.RealResampler;
import io.github.dsheirer.dsp.fm.SquelchingFMDemodulator;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.PrimaryDecoder;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.IReusableBufferProvider;
import io.github.dsheirer.sample.buffer.IReusableComplexBufferListener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableFloatBuffer;
import io.github.dsheirer.source.ISourceEventListener;
import io.github.dsheirer.source.ISourceEventProvider;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Decoder module with integrated narrowband FM (12.5 or 25.0 kHz channel) demodulator
 */
public class NBFMDecoder extends PrimaryDecoder implements ISourceEventListener, ISourceEventProvider,
		IReusableComplexBufferListener, Listener<ReusableComplexBuffer>, IReusableBufferProvider,
		IDecoderStateEventProvider
{
	private final static Logger mLog = LoggerFactory.getLogger(NBFMDecoder.class);
	private static final double DEMODULATED_AUDIO_SAMPLE_RATE = 8000.0;
	private static final double POWER_SQUELCH_ALPHA_DECAY = 0.0004;
	private static final double POWER_SQUELCH_THRESHOLD_DB = -78.0;
	private static final int POWER_SQUELCH_RAMP = 4;

	private ComplexFIRFilter2 mIQFilter;
	private IComplexDecimationFilter mDecimationFilter;
	private SquelchingFMDemodulator mDemodulator = new SquelchingFMDemodulator(POWER_SQUELCH_ALPHA_DECAY,
			POWER_SQUELCH_THRESHOLD_DB, POWER_SQUELCH_RAMP);
	private RealResampler mResampler;
	private SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
	private Listener<ReusableFloatBuffer> mResampledReusableBufferListener;
	private Listener<DecoderStateEvent> mDecoderStateEventListener;
	private double mChannelBandwidth;
	private double mOutputSampleRate = DEMODULATED_AUDIO_SAMPLE_RATE;
	private boolean mSquelch = true;

	/**
	 * Constructs an instance
	 * @param config to setup the decoder
	 */
	public NBFMDecoder( DecodeConfigNBFM config )
	{
		super( config );
		mChannelBandwidth = config.getBandwidth().getValue();
		mDemodulator.setSquelchThreshold(config.getSquelchThreshold());
	}

	@Override
    public DecoderType getDecoderType()
    {
	    return DecoderType.NBFM;
    }

	@Override
	public Listener<ReusableComplexBuffer> getReusableComplexBufferListener()
	{
		return this;
	}

	@Override
	public Listener<SourceEvent> getSourceEventListener()
	{
		return mSourceEventProcessor;
	}

	@Override
	public void reset()
	{
		mDemodulator.reset();
	}

	@Override
	public void start()
	{
	}

	@Override
	public void stop()
	{
	}

	@Override
	public void setBufferListener(Listener<ReusableFloatBuffer> listener)
	{
		mResampledReusableBufferListener = listener;
	}

	@Override
	public void removeBufferListener()
	{
		mResampledReusableBufferListener = null;
	}

	@Override
	public void receive(ReusableComplexBuffer reusableComplexBuffer)
	{
		if(mIQFilter == null)
		{
			reusableComplexBuffer.decrementUserCount();
			throw new IllegalStateException("NBFM demodulator module must receive a sample rate change source " +
					"event before it can process complex sample buffers");
		}

		ReusableComplexBuffer decimatedBuffer = mDecimationFilter.decimate(reusableComplexBuffer);
		ReusableComplexBuffer basebandFilteredBuffer = mIQFilter.filter(decimatedBuffer);

		ReusableFloatBuffer demodulatedBuffer = mDemodulator.demodulate(basebandFilteredBuffer);

		if(mResampler != null)
		{
			//If we're currently squelched and the squelch state changed while demodulating the baseband samples,
			// then un-squelch so we can send this buffer
			if(mSquelch && mDemodulator.isSquelchChanged())
			{
				mSquelch = false;
				notifyCallStart();
			}

			//Either send the demodulated buffer to the resampler for distro, or decrement the user count
			if(mSquelch)
			{
				demodulatedBuffer.incrementUserCount();
				notifyIdle();
			}
			else
			{
				mResampler.resample(demodulatedBuffer);
				notifyCallContinuation();
			}

			//Set to squelch if necessary to close out the audio buffers
			if(!mSquelch && mDemodulator.isMuted())
			{
				mSquelch = true;
				notifyCallEnd();
			}
		}
		else
		{
			demodulatedBuffer.decrementUserCount();
			notifyIdle();
		}
	}

	/**
	 * Broadcasts a call start state event
	 */
	private void notifyCallStart()
	{
		broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.START, State.CALL, 0));
	}

	/**
	 * Broadcasts a call continuation state event
	 */
	private void notifyCallContinuation()
	{
		broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.CALL, 0));
	}

	/**
	 * Broadcasts a call end state event
	 */
	private void notifyCallEnd()
	{
		broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.END, State.CALL, 0));
	}

	/**
	 * Broadcasts an idle notification
	 */
	private void notifyIdle()
	{
		broadcast(new DecoderStateEvent(this, DecoderStateEvent.Event.CONTINUATION, State.IDLE, 0));
	}

	/**
	 * Broadcasts the decoder state event to an optional registered listener
	 */
	private void broadcast(DecoderStateEvent event)
	{
		if(mDecoderStateEventListener != null)
		{
			mDecoderStateEventListener.receive(event);
		}
	}

	/**
	 * Sets the decoder state listener
	 */
	@Override
	public void setDecoderStateListener(Listener<DecoderStateEvent> listener)
	{
		mDecoderStateEventListener = listener;
	}

	/**
	 * Removes the decoder state event listener
	 */
	@Override
	public void removeDecoderStateListener()
	{
		mDecoderStateEventListener = null;
	}

	/**
	 * Registers the listener to receive squelch change events from the demodulator/squelch controller.
	 */
	@Override
	public void setSourceEventListener(Listener<SourceEvent> listener)
	{
		mDemodulator.setSourceEventListener(listener);
	}

	/**
	 * De-registers the listener
	 */
	@Override
	public void removeSourceEventListener()
	{
		mDemodulator.setSourceEventListener(null);
	}

	/**
	 * Monitors sample rate change source event(s) to setup the initial I/Q filter and passes squelch threshold
	 * change requests down to the demodulator.
	 */
	public class SourceEventProcessor implements Listener<SourceEvent>
	{
		@Override
		public void receive(SourceEvent sourceEvent)
		{
			if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE)
			{
				if(mIQFilter != null)
				{
					mIQFilter.dispose();
					mIQFilter = null;
				}

				double sampleRate = sourceEvent.getValue().doubleValue();

				int decimationRate = 0;
				double decimatedSampleRate = sampleRate;

				if(sampleRate / 2 >= (mChannelBandwidth * 2))
				{
					decimationRate = 2;

					while(sampleRate / decimationRate / 2 >= (mChannelBandwidth * 2))
					{
						decimationRate *= 2;
					}
				}

				if(decimationRate > 0)
				{
					decimatedSampleRate /= decimationRate;
				}

				mDecimationFilter = DecimationFilterFactory.getComplexDecimationFilter(decimationRate);

				if((decimatedSampleRate < (2.0 * mChannelBandwidth)))
				{
					throw new IllegalStateException("FM Demodulator with channel bandwidth [" + mChannelBandwidth +
							"] requires a channel sample rate of [" + (2.0 * mChannelBandwidth + "] - sample rate of [" +
							decimatedSampleRate + "] is not supported"));
				}

				int passBandStop = (int)(mChannelBandwidth * .8);
				int stopBandStart = (int)mChannelBandwidth;

				float[] filterTaps = null;

				FIRFilterSpecification specification = FIRFilterSpecification.lowPassBuilder()
						.sampleRate(decimatedSampleRate * 2)
						.gridDensity(16)
						.oddLength(true)
						.passBandCutoff(passBandStop)
						.passBandAmplitude(1.0)
						.passBandRipple(0.01)
						.stopBandStart(stopBandStart)
						.stopBandAmplitude(0.0)
						.stopBandRipple(0.005) //Approximately 90 dB attenuation
						.build();

				try
				{
					filterTaps = FilterFactory.getTaps(specification);
				}
				catch(FilterDesignException fde)
				{
					mLog.error("Couldn't design FM demodulator remez filter for sample rate [" + sampleRate +
							"] pass frequency [" + passBandStop + "] and stop frequency [" + stopBandStart +
							"] - will proceed using sinc (low-pass) filter");
				}

				if(filterTaps == null)
				{
					mLog.info("Unable to use remez filter designer for sample rate [" + decimatedSampleRate +
							"] pass band stop [" + passBandStop +
							"] and stop band start [" + stopBandStart + "] - will proceed using simple low pass filter design");
					filterTaps = FilterFactory.getLowPass(decimatedSampleRate, passBandStop, stopBandStart, 60,
							Window.WindowType.HAMMING, true);
				}

				mIQFilter = new ComplexFIRFilter2(filterTaps);

				mResampler = new RealResampler(decimatedSampleRate, mOutputSampleRate, 2000, 1000);

				mResampler.setListener(reusableFloatBuffer ->
				{
					if(mResampledReusableBufferListener != null)
					{
						mResampledReusableBufferListener.receive(reusableFloatBuffer);
					}
					else
					{
						reusableFloatBuffer.decrementUserCount();
					}
				});
			}
			else if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CHANGE_SQUELCH_THRESHOLD)
			{
				//Send request for squelch threshold change to the demodulator/squelch controller.
				mDemodulator.receive(sourceEvent);
			}
			else if(sourceEvent.getEvent() == SourceEvent.Event.REQUEST_CURRENT_SQUELCH_THRESHOLD)
			{
				mDemodulator.receive(sourceEvent);
			}
		}
	}
}
