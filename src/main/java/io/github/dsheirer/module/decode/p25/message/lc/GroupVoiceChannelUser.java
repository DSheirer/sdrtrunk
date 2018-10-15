package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Current user of an APCO25 channel on both inbound and outbound channels
 */
public class GroupVoiceChannelUser extends LinkControlWord
{
    private static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] GROUP_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] SOURCE_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    private ServiceOptions mServiceOptions;
    private IIdentifier mGroupAddress;
    private IIdentifier mSourceAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public GroupVoiceChannelUser(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" ").append(getServiceOptions());
        return sb.toString();
    }

    /**
     * Service Options for this channel
     */
    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    /**
     * Talkgroup address
     */
    public IIdentifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ADDRESS));
        }

        return mGroupAddress;
    }

    /**
     * Source address
     */
    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
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
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
