package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended function command to a target address
 */
public class ExtendedFunctionCommand extends LinkControlWord
{
    private static final int[] EXTENDED_FUNCTION = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24,
            25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47};
    private static final int[] TARGET_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61, 62, 63, 64,
            65, 66, 67, 68, 69, 70, 71};

    private IIdentifier mTargetAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public ExtendedFunctionCommand(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FUNCTION:").append(getExtendedFunction());
        return sb.toString();
    }

    public String getExtendedFunction()
    {
        return getMessage().getHex(EXTENDED_FUNCTION, 10);
    }

    /**
     * Talkgroup address
     */
    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
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
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
