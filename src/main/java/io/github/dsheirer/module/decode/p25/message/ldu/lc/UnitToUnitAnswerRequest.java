package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.ServiceOptions;

public class UnitToUnitAnswerRequest extends LDU1Message
{
    public static final int[] SERVICE_OPTIONS = {376, 377, 382, 383, 384, 385, 386, 387};
    public static final int[] TARGET_ADDRESS = {536, 537, 538, 539, 540, 541, 546, 547, 548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] SOURCE_ADDRESS = {720, 721, 722, 723, 724, 725, 730, 731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private ServiceOptions mServiceOptions;
    private IIdentifier mTargetAddress;
    private IIdentifier mSourceAddress;

    public UnitToUnitAnswerRequest(LDU1Message message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());
        sb.append(" ").append(getServiceOptions());
        sb.append(" FROM:" + getSourceAddress());
        sb.append(" TO:" + getTargetAddress());

        return sb.toString();
    }

    public ServiceOptions getServiceOptions()
    {
        if(mServiceOptions == null)
        {
            mServiceOptions = new ServiceOptions(mMessage.getInt(SERVICE_OPTIONS));
        }

        return mServiceOptions;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    public IIdentifier getSourceAddress()
    {
        if(mSourceAddress == null)
        {
            mSourceAddress = APCO25FromTalkgroup.createIndividual(mMessage.getInt(SOURCE_ADDRESS));
        }

        return mSourceAddress;
    }
}
