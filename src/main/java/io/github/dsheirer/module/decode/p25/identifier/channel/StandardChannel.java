package io.github.dsheirer.module.decode.p25.identifier.channel;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.protocol.Protocol;

import java.text.DecimalFormat;

/**
 * Standard AM/FM channel with discrete frequency
 */
public class StandardChannel implements IChannelDescriptor
{
    private static final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.000");
    private long mFrequency;

    /**
     * Constructs an instance
     * @param frequency of the channel (Hz)
     */
    public StandardChannel(long frequency)
    {
        mFrequency = frequency;
    }

    @Override
    public long getDownlinkFrequency()
    {
        return mFrequency;
    }

    @Override
    public long getUplinkFrequency()
    {
        return mFrequency;
    }

    @Override
    public int[] getFrequencyBandIdentifiers()
    {
        return new int[0];
    }

    @Override
    public void setFrequencyBand(IFrequencyBand bandIdentifier)
    {
        //no-op
    }

    @Override
    public boolean isTDMAChannel()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 0;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.UNKNOWN;
    }

    @Override
    public String toString()
    {
        return FREQUENCY_FORMATTER.format(mFrequency / 1E6d) + " MHz";
    }
}
