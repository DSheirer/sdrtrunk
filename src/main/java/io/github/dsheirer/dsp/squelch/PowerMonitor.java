package io.github.dsheirer.dsp.squelch;

import io.github.dsheirer.dsp.filter.iir.SinglePoleIirFilter;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.SourceEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Power monitor.  Provides periodic broadcast of current channel power from I/Q sample buffers.
 */
public class PowerMonitor
{
    private static final Logger mLog = LoggerFactory.getLogger(PowerMonitor.class);
    private int mPowerLevelBroadcastCount = 0;
    private int mPowerLevelBroadcastThreshold;
    private Listener<SourceEvent> mSourceEventListener;
    private SinglePoleIirFilter mPowerFilter = new SinglePoleIirFilter(0.1);

    /**
     * Constructs an instance
     */
    public PowerMonitor()
    {
        mPowerLevelBroadcastThreshold = 25000; //Based on a default sample rate of 50 kHz
    }

    /**
     * Sets the sample rate to effect the frequency of power level notifications where the notifications are
     * sent twice a second.
     * @param sampleRate in hertz
     */
    public void setSampleRate(int sampleRate)
    {
        mPowerLevelBroadcastThreshold = sampleRate / 2;
    }

    /**
     * Processes a complex IQ sample and changes squelch state when the signal power is above or below the
     * threshold value.
     * @param inphase complex sample component
     * @param quadrature complex sample component
     */
    public void process(double inphase, double quadrature)
    {
        mPowerLevelBroadcastCount++;

        if(mPowerLevelBroadcastCount > mPowerLevelBroadcastThreshold)
        {
            mPowerFilter.filter(inphase * inphase + quadrature * quadrature);
        }

        if(mPowerLevelBroadcastCount > (mPowerLevelBroadcastThreshold + 10))
        {
            mPowerLevelBroadcastCount = 0;
            broadcast(SourceEvent.channelPowerLevel(null, 10.0 * Math.log10(mPowerFilter.getValue())));
        }
    }

    /**
     * Processes a complex baseband sample buffer.  Note: this method does not decrement the user count
     * on the buffer.
     *
     * @param buffer to process
     */
    public void process(ReusableComplexBuffer buffer)
    {
        float[] samples = buffer.getSamples();
        int offset;

        for(int x = 0; x < buffer.getSampleCount(); x++)
        {
            offset = x * 2;
            process(samples[offset], samples[offset + 1]);
        }
    }

    /**
     * Registers the listener to receive power level notifications and squelch threshold requests
     */
    public void setSourceEventListener(Listener<SourceEvent> listener)
    {
        mSourceEventListener = listener;
    }

    /**
     * Broadcasts the source event to an optional register listener
     */
    private void broadcast(SourceEvent event)
    {
        if(mSourceEventListener != null)
        {
            mSourceEventListener.receive(event);
        }
    }
}
