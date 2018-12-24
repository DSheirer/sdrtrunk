package io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.ExtendedFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Extended function response.
 */
public class ExtendedFunctionCommand extends OSPMessage
{
    private static final int[] FUNCTION = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ARGUMENTS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50,
            51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71,
            72, 73, 74, 75, 76, 77, 78, 79};

    private ExtendedFunction mExtendedFunction;
    private String mArguments;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public ExtendedFunctionCommand(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" FUNCTION:").append(getExtendedFunction());
        sb.append(" ARGUMENTS:").append(getArguments());
        return sb.toString();
    }

    public ExtendedFunction getExtendedFunction()
    {
        if(mExtendedFunction == null)
        {
            mExtendedFunction = ExtendedFunction.fromValue(getMessage().getInt(FUNCTION));
        }

        return mExtendedFunction;
    }

    public String getArguments()
    {
        if(mArguments == null)
        {
            mArguments = getMessage().getHex(ARGUMENTS, 6).toUpperCase();
        }

        return mArguments;
    }

    public Identifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
