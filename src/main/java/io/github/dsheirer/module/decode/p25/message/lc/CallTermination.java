package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25AnyTalkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Termination or cancellation of a call and release of the channel.
 */
public class CallTermination extends LinkControlWord
{
    private static final int[] ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66,
            67, 68, 69, 70, 71};

    private IIdentifier mAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public CallTermination(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" ADDRESS:").append(getAddress());
        return sb.toString();
    }

    /**
     * To/From radio identifier communicating with a landline
     */
    public IIdentifier getAddress()
    {
        if(mAddress == null)
        {
            mAddress = APCO25AnyTalkgroup.create(getMessage().getInt(ADDRESS));
        }

        return mAddress;
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
            mIdentifiers.add(getAddress());
        }

        return mIdentifiers;
    }
}
