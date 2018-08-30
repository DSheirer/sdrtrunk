package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.node.APCO25System;
import io.github.dsheirer.identifier.integer.node.APCO25Wacn;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;

public class UnitRegistrationCommand extends LDU1Message
{
    public static final int[] WACN_ID = {364, 365, 366, 367, 372, 373, 374, 375, 376, 377, 382, 383, 384, 385, 386, 387,
        536, 537, 538, 539};
    public static final int[] SYSTEM_ID = {540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559};
    public static final int[] TARGET_ID = {560, 561, 566, 567, 568, 569, 570, 571, 720, 721, 722, 723, 724, 725, 730,
        731, 732, 733, 734, 735, 740, 741, 742, 743};

    private IIdentifier mWACN;
    private IIdentifier mSystem;
    private IIdentifier mTargetAddress;

    public UnitRegistrationCommand(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ADDRESS:" + getCompleteTargetAddress());

        return sb.toString();
    }

    public String getCompleteTargetAddress()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getWACN());
        sb.append(":");
        sb.append(getSystemID());
        sb.append(":");
        sb.append(getTargetID());

        return sb.toString();
    }

    public IIdentifier getWACN()
    {
        if(mWACN == null)
        {
            mWACN = APCO25Wacn.create(mMessage.getInt(WACN_ID));
        }

        return mWACN;
    }

    public IIdentifier getSystemID()
    {
        if(mSystem == null)
        {
            mSystem = APCO25System.create(mMessage.getInt(SYSTEM_ID));
        }

        return mSystem;
    }

    public IIdentifier getTargetID()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ID));
        }

        return mTargetAddress;
    }
}
