package io.github.dsheirer.module.decode.p25.phase1.message;

import org.apache.commons.math3.util.FastMath;

/**
 * Utility class to declare a frequency band
 */
public class P25FrequencyBand implements IFrequencyBand
{
    private int mIdentifier;
    private long mBaseFrequency;
    private long mTransmitOffset;
    private long mChannelSpacing;
    private int mBandwidth;
    private int mTimeslotCount;

    /**
     * Construct an instance
     * @param identifier for this frequency band
     * @param baseFrequency in Hertz
     * @param transmitOffset in Hertz
     * @param channelSpacing in Hertz
     * @param bandwidth in Hertz
     * @param timeslotCount for number of timeslots, 1 or 2
     */
    public P25FrequencyBand(int identifier, long baseFrequency, long transmitOffset, long channelSpacing,
                            int bandwidth, int timeslotCount)
    {
        mIdentifier = identifier;
        mChannelSpacing = channelSpacing;
        mBaseFrequency = baseFrequency;
        mBandwidth = bandwidth;
        mTransmitOffset = transmitOffset;
        mTimeslotCount = timeslotCount;
    }

    @Override
    public int getIdentifier()
    {
        return mIdentifier;
    }

    @Override
    public long getChannelSpacing()
    {
        return mChannelSpacing;
    }

    @Override
    public long getBaseFrequency()
    {
        return mBaseFrequency;
    }

    @Override
    public int getBandwidth()
    {
        return mBandwidth;
    }

    @Override
    public long getTransmitOffset()
    {
        return mTransmitOffset;
    }

    @Override
    public long getDownlinkFrequency(int channelNumber)
    {
        if(isTDMA())
        {
            return getBaseFrequency() + (getChannelSpacing() * (int)(FastMath.floor(channelNumber / getTimeslotCount())));
        }

        return getBaseFrequency() + (getChannelSpacing() * channelNumber);
    }

    @Override
    public long getUplinkFrequency(int channelNumber)
    {
        return getDownlinkFrequency(channelNumber) + getTransmitOffset();
    }

    @Override
    public boolean isTDMA()
    {
        return getTimeslotCount() > 1;
    }

    @Override
    public int getTimeslotCount()
    {
        return mTimeslotCount;
    }
}
