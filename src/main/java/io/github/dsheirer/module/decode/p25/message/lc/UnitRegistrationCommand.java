package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit registration command
 */
public class UnitRegistrationCommand extends LinkControlWord
{
    private static final int[] WACN = {8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27};
    private static final int[] SYSTEM_ID = {28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39};
    private static final int[] TARGET_ADDRESS = {40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56,
            57, 58, 59, 60, 61, 62, 63};
    private static final int[] RESERVED = {64, 65, 66, 67, 68, 69, 70, 71};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mTargetAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public UnitRegistrationCommand(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" WACN:").append(getWACN());
        sb.append(" SYSTEM:").append(getSystem());
        sb.append(" RADIO:").append(getTargetAddress());
        return sb.toString();
    }

    /**
     * WACN
     */
    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(getMessage().getInt(WACN));
        }

        return mWACN;
    }

    /**
     * System
     */
    public IIdentifier getSystem()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(getMessage().getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    /**
     * Source address
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
            mIdentifiers.add(getWACN());
            mIdentifiers.add(getSystem());
            mIdentifiers.add(getTargetAddress());
        }

        return mIdentifiers;
    }
}
