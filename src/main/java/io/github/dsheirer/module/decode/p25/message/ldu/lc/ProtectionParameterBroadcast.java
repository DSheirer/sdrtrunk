package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.Encryption;

public class ProtectionParameterBroadcast extends LDU1Message
{
    public static final int[] ALGORITHM_ID = {536, 537, 538, 539, 540, 541, 546, 547};
    public static final int[] KEY_ID = {548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] TARGET_ADDRESS = {720, 721, 722, 723, 724, 725, 730, 731, 732, 733, 734, 735, 740, 741,
        742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    private IIdentifier mTargetAddress;

    public ProtectionParameterBroadcast(LDU1Message message)
    {
        super(message);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" ENCRYPTION:" + getEncryption().name());
        sb.append(" KEY:" + getEncryptionKey());
        sb.append(" ADDRESSS:" + getTargetAddress());

        return sb.toString();
    }

    public Encryption getEncryption()
    {
        return Encryption.fromValue(mMessage.getInt(ALGORITHM_ID));
    }

    public int getEncryptionKey()
    {
        return mMessage.getInt(KEY_ID);
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(mMessage.getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }
}
