package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBand;

import java.util.Collections;
import java.util.List;

/**
 * Echo of the status update from a subscriber when the destination of the update is another subscriber unit.
 */
public class ChannelIdentifierUpdateExplicit extends LinkControlWord implements IFrequencyBand
{
    private static final int[] FREQUENCY_BAND_IDENTIFIER = {8, 9, 10, 11};
    private static final int[] BANDWIDTH = {12, 13, 14, 15};
    private static final int[] TRANSMIT_OFFSET = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
    private static final int[] CHANNEL_SPACING = {30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] BASE_FREQUENCY = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public ChannelIdentifierUpdateExplicit(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ID:").append(getIdentifier());
        sb.append(" BW:").append(getBandwidth());
        sb.append(" OFFSET:").append(getTransmitOffset());
        sb.append(" SPACING:").append(getChannelSpacing());
        sb.append(" BASE:").append(getBaseFrequency());
        return sb.toString();
    }

    @Override
    public int getIdentifier()
    {
        return getMessage().getInt(FREQUENCY_BAND_IDENTIFIER);
    }

    @Override
    public long getChannelSpacing()
    {
        return getMessage().getInt(CHANNEL_SPACING) * 125l;
    }

    @Override
    public long getBaseFrequency()
    {
        return getMessage().getLong(BASE_FREQUENCY) * 5l;
    }

    @Override
    public int getBandwidth()
    {
        return getMessage().getInt(BANDWIDTH) * 125;
    }

    @Override
    public long getTransmitOffset()
    {
        return -1 * getMessage().getLong(TRANSMIT_OFFSET) * 250000l;
    }

    @Override
    public long getDownlinkFrequency(int channelNumber)
    {
        return getBaseFrequency() + (channelNumber * getChannelSpacing());
    }

    @Override
    public long getUplinkFrequency(int channelNumber)
    {
        return getDownlinkFrequency(channelNumber) + getTransmitOffset();
    }

    @Override
    public boolean isTDMA()
    {
        return false;
    }

    @Override
    public int getTimeslotCount()
    {
        return 1;
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
