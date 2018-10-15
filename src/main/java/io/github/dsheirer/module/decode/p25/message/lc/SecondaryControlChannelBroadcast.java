package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25Channel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast information.
 */
public class SecondaryControlChannelBroadcast extends LinkControlWord implements FrequencyBandReceiver
{
    private static final int[] RFSS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SITE = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] FREQUENCY_BAND_A = {24, 25, 26, 27};
    private static final int[] CHANNEL_NUMBER_A = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] SERVICE_CLASS_A = {40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] FREQUENCY_BAND_B = {48, 49, 50, 51};
    private static final int[] CHANNEL_NUMBER_B = {52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] SERVICE_CLASS_B = {64, 65, 66, 67, 68, 69, 70, 71};

    private List<IIdentifier> mIdentifiers;
    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannelA;
    private IAPCO25Channel mChannelB;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public SecondaryControlChannelBroadcast(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" SITE:" + getRFSS() + "-" + getSite());
        sb.append(" CHAN A:" + getChannelA());
        sb.append(" " + SystemService.toString(getSystemServiceClassA()));
        sb.append(" CHAN B:" + getChannelB());
        sb.append(" " + SystemService.toString(getSystemServiceClassB()));
        return sb.toString();
    }

    public IIdentifier getRFSS()
    {
        if(mRFSS == null)
        {
            mRFSS = APCO25Rfss.create(getMessage().getInt(RFSS));
        }

        return mRFSS;
    }

    public IIdentifier getSite()
    {
        if(mSite == null)
        {
            mSite = APCO25Site.create(getMessage().getInt(SITE));
        }

        return mSite;
    }

    public IAPCO25Channel getChannelA()
    {
        if(mChannelA == null)
        {
            mChannelA = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_A),
                    getMessage().getInt(CHANNEL_NUMBER_A));
        }

        return mChannelA;
    }

    public IAPCO25Channel getChannelB()
    {
        if(mChannelB == null)
        {
            mChannelB = APCO25Channel.create(getMessage().getInt(FREQUENCY_BAND_B),
                    getMessage().getInt(CHANNEL_NUMBER_B));
        }

        return mChannelB;
    }

    public int getSystemServiceClassA()
    {
        return getMessage().getInt(SERVICE_CLASS_A);
    }

    public int getSystemServiceClassB()
    {
        return getMessage().getInt(SERVICE_CLASS_B);
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getRFSS());
            mIdentifiers.add(getSite());
        }

        return mIdentifiers;
    }

    @Override
    public List<IAPCO25Channel> getChannels()
    {
        List<IAPCO25Channel> channels = new ArrayList<>();
        channels.add(getChannelA());
        channels.add(getChannelB());
        return channels;
    }
}
