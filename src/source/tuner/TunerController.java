/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2017 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package source.tuner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Broadcaster;
import sample.Listener;
import sample.complex.ComplexBuffer;
import sample.complex.IComplexBufferProvider;
import source.ISourceEventListener;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceEventListenerToProcessorAdapter;
import source.SourceException;
import source.tuner.channel.ChannelManager;
import source.tuner.channel.cic.CICTunerChannelSource;
import source.tuner.channel.TunerChannel;
import source.tuner.channel.polyphase.PolyphaseChannelManager;
import source.tuner.configuration.TunerConfiguration;
import source.tuner.channel.FrequencyController;
import source.tuner.channel.FrequencyController.Tunable;

import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class TunerController implements Tunable, ISourceEventProcessor, ISourceEventListener,
    IComplexBufferProvider, Listener<ComplexBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(TunerController.class);
    protected Broadcaster<ComplexBuffer> mSampleBroadcaster = new Broadcaster<>();

    /* List of currently tuned channels being served to demod channels */
    private SortedSet<TunerChannel> mTunedChannels = new ConcurrentSkipListSet<>();
    private FrequencyController mFrequencyController;
    private ChannelManager mChannelManager;
    private Listener<SourceEvent> mSourceEventListener;

    /**
     * Abstract tuner controller class.  The tuner controller manages frequency bandwidth and currently tuned channels
     * that are being fed samples from the tuner.
     *
     * @param minimumFrequency minimum uncorrected tunable frequency
     * @param maximumFrequency maximum uncorrected tunable frequency
     * @param middleUnusable is the +/- value center DC spike to avoid for channels
     * @param usableBandwidth percentage of usable bandwidth relative to space at the extreme ends of the spectrum
     * @throws SourceException - for any issues related to constructing the
     *                         class, tuning a frequency, or setting the bandwidth
     */
    public TunerController(long minimumFrequency, long maximumFrequency, long middleUnusable, double usableBandwidth)
    {
        mFrequencyController = new FrequencyController(this, minimumFrequency, maximumFrequency,
            0.0d, middleUnusable, usableBandwidth);

        mChannelManager = new PolyphaseChannelManager(mFrequencyController, this);

        mSourceEventListener = new SourceEventListenerToProcessorAdapter(this);
    }

    /**
     * Frequency controller for managing frequency and sample rate.
     */
    public FrequencyController getFrequencyController()
    {
        return mFrequencyController;
    }

    /**
     * Channel manager for managing Digital Drop Chanel (DDC) tuner channel sources
     */
    public ChannelManager getChannelManager()
    {
        return mChannelManager;
    }

    /**
     * Implements the ISourceEventListener interface to receive requests from sample consumers
     */
    @Override
    public Listener<SourceEvent> getSourceEventListener()
    {
        return mSourceEventListener;
    }

    /**
     * Applies the settings in the tuner configuration
     */
    public abstract void apply(TunerConfiguration config) throws SourceException;

    /**
     * Responds to requests to set the frequency
     */
    @Override
	public void process(SourceEvent sourceEvent ) throws SourceException
	{
	    switch(sourceEvent.getEvent())
        {
            case REQUEST_FREQUENCY_CHANGE:
                getFrequencyController().setFrequency( sourceEvent.getValue().longValue() );
                break;
            case REQUEST_START_SAMPLE_STREAM:
                if(sourceEvent.getSource() instanceof CICTunerChannelSource)
                {
                    addComplexBufferListener((CICTunerChannelSource)sourceEvent.getSource());
                }
                break;
            case REQUEST_STOP_SAMPLE_STREAM:
                if(sourceEvent.getSource() instanceof CICTunerChannelSource)
                {
                    removeComplexBufferListener((CICTunerChannelSource)sourceEvent.getSource());
                }
                break;
            default:
                mLog.error("Ignoring unrecognized source event: " + sourceEvent.getEvent().name() + " from [" +
                    sourceEvent.getSource().getClass() + "]" );
        }
	}
    @Override
    public boolean canTune(long frequency)
    {
        return getFrequencyController().canTune(frequency);
    }

	/**
	 * Sets the listener to be notified any time that the tuner changes frequency
	 * or bandwidth/sample rate.
	 * 
	 * Note: this is normally used by the Tuner.  Any additional listeners can
	 * be registered on the tuner.
	 */
    public void addListener( ISourceEventProcessor processor )
    {
        mFrequencyController.addListener(processor);
    }

    /**
     * Removes the frequency change listener
     */
    public void removeListener( ISourceEventProcessor processor )
    {
        mFrequencyController.removeFrequencyChangeProcessor(processor);
    }

    /**
     * Adds the listener to receive complex buffer samples
     */
    @Override
    public void addComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mSampleBroadcaster.addListener(listener);
    }

    /**
     * Removes the listener from receiving complex buffer samples
     */
    @Override
    public void removeComplexBufferListener(Listener<ComplexBuffer> listener)
    {
        mSampleBroadcaster.removeListener(listener);
    }

    /**
     * Indicates if there are any complex buffer listeners registered on this controller
     */
    @Override
    public boolean hasComplexBufferListeners()
    {
        return mSampleBroadcaster.hasListeners();
    }

    /**
     * Broadcasts the buffer to any registered listeners
     */
    protected void broadcast(ComplexBuffer complexBuffer)
    {
        mSampleBroadcaster.broadcast(complexBuffer);
    }

    /**
     * Implements the Listener<T> interface to receive and distribute complex buffers from subclass implementations
     */
    @Override
    public void receive(ComplexBuffer complexBuffer)
    {
        broadcast(complexBuffer);
    }
}
