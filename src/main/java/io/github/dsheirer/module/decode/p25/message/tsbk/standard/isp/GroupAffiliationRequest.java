package io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.APCO25System;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;

import java.util.ArrayList;
import java.util.List;

/**
 * Group affiliation request
 */
public class GroupAffiliationRequest extends ISPMessage
{
    private static final int[] RESERVED = {16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] SYSTEM_ID = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] GROUP_ID = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private Identifier mSystemId;
    private Identifier mGroupAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public GroupAffiliationRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" TO:").append(getGroupAddress());
        sb.append(" SYSTEM:").append(getSystemId());
        return sb.toString();
    }

    public Identifier getSystemId()
    {
        if(mSystemId == null)
        {
            mSystemId = APCO25System.create(getMessage().getInt(SYSTEM_ID));
        }

        return mSystemId;
    }

    public Identifier getGroupAddress()
    {
        if(mGroupAddress == null)
        {
            mGroupAddress = APCO25ToTalkgroup.createGroup(getMessage().getInt(GROUP_ID));
        }

        return mGroupAddress;
    }

    public Identifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(getMessage().getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getSystemId());
            mIdentifiers.add(getGroupAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
