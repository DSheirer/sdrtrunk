package io.github.dsheirer.module.decode.p25.message.tsbk2.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk2.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit registration request
 */
public class UnitDeRegistrationRequest extends ISPMessage
{
    private static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] WACN = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43};
    private static final int[] SYSTEM = {44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ID = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mSourceAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public UnitDeRegistrationRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        return sb.toString();
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(getMessage().getInt(WACN));
        }

        return mWACN;
    }

    public IIdentifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM));
        }

        return mSystem;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ID));
        }

        return mSourceAddress;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getWACN());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
