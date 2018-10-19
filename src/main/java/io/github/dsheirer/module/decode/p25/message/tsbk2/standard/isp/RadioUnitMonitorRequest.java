package io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk2.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Radio unit monitor request
 */
public class RadioUnitMonitorRequest extends ISPMessage
{
    private static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29};
    private static final int[] TX_MULTIPLIER = {30, 31};
    private static final int[] TARGET_ADDRESS = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48, 49,
            50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private IIdentifier mTargetAddress;
    private IIdentifier mSourceAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public RadioUnitMonitorRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" TX MULTIPLIER:").append(getTxMultiplier());
        return sb.toString();
    }

    /**
     * Multipler for a value stored in the radio that determines how long the target address radio will be
     * keyed for monitoring.
     */
    public int getTxMultiplier()
    {
        return getMessage().getInt(TX_MULTIPLIER);
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
