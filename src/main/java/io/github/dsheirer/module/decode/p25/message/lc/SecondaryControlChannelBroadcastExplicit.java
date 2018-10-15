package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.channel.APCO25ExplicitChannel;
import io.github.dsheirer.identifier.integer.channel.IAPCO25Channel;
import io.github.dsheirer.identifier.integer.node.APCO25Rfss;
import io.github.dsheirer.identifier.integer.node.APCO25Site;
import io.github.dsheirer.module.decode.p25.message.FrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.message.tsbk.osp.control.SystemService;

import java.util.ArrayList;
import java.util.List;

/**
 * Secondary control channel broadcast explicit information for V/UHF channels
 */
public class SecondaryControlChannelBroadcastExplicit extends LinkControlWord implements FrequencyBandReceiver
{
    private static final int[] RFSS = {8, 9, 10, 11, 12, 13, 14, 15};
    private static final int[] SITE = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] DOWNLINK_FREQUENCY_BAND = {24, 25, 26, 27};
    private static final int[] DOWNLINK_CHANNEL_NUMBER = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] UPLINK_FREQUENCY_BAND = {40, 41, 42, 43};
    private static final int[] UPNLINK_CHANNEL_NUMBER = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SERVICE_CLASS = {56, 57, 58, 59, 60, 61, 62, 63};
    private static final int[] RESERVED = {64, 65, 66, 67, 68, 69, 70, 71};

    private List<IIdentifier> mIdentifiers;
    private IIdentifier mRFSS;
    private IIdentifier mSite;
    private IAPCO25Channel mChannel;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public SecondaryControlChannelBroadcastExplicit(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" SITE:" + getRFSS() + "-" + getSite());
        sb.append(" CHAN:" + getChannel());
        sb.append(" " + SystemService.toString(getSystemServiceClass()));
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

    public IAPCO25Channel getChannel()
    {
        if(mChannel == null)
        {
            mChannel = APCO25ExplicitChannel.create(getMessage().getInt(DOWNLINK_FREQUENCY_BAND),
                    getMessage().getInt(DOWNLINK_CHANNEL_NUMBER), getMessage().getInt(UPLINK_FREQUENCY_BAND),
                    getMessage().getInt(UPNLINK_CHANNEL_NUMBER));
        }

        return mChannel;
    }

    public int getSystemServiceClass()
    {
        return getMessage().getInt(SERVICE_CLASS);
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
        channels.add(getChannel());
        return channels;
    }
}
