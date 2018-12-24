package io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice channel grant update.
 */
public class GroupDataChannelAnnouncement extends OSPMessage implements IFrequencyBandReceiver
{
    private static final int[] FREQUENCY_BAND_A = {16, 17, 18, 19};
    private static final int[] CHANNEL_NUMBER_A = {20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] GROUP_ADDRESS_A = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FREQUENCY_BAND_B = {48, 49, 50, 51};
    private static final int[] CHANNEL_NUMBER_B = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] GROUP_ADDRESS_B = {64, 65, 66, 67, 68, 69, 70, 71, 72, 73, 74, 75, 76, 77, 78, 79};

    private IChannelDescriptor mChannelA;
    private Identifier mGroupAddressA;
    private IChannelDescriptor mChannelB;
    private Identifier mGroupAddressB;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public GroupDataChannelAnnouncement(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" GROUP A:").append(getGroupAddressA());
        sb.append(" CHAN:").append(getChannelA());
        sb.append(" GROUP B:").append(getGroupAddressB());
        sb.append(" CHAN:").append(getChannelB());
        return sb.toString();
    }

    public IChannelDescriptor getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A),
                    getMessage().getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    public Identifier getGroupAddressA()
    {
        if(mGroupAddressA == null)
        {
            mGroupAddressA = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_A));
        }

        return mGroupAddressA;
    }

    public IChannelDescriptor getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B),
                    getMessage().getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public Identifier getGroupAddressB()
    {
        if(mGroupAddressB == null)
        {
            mGroupAddressB = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS_B));
        }

        return mGroupAddressB;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getGroupAddressA());
            mIdentifiers.add(getGroupAddressB());
        }

        return mIdentifiers;
    }

    @Override
    public List<IChannelDescriptor> getChannels()
    {
        List<IChannelDescriptor> channels = new ArrayList<>();
        channels.add(getChannelA());
        channels.add(getChannelB());
        return channels;
    }
}
