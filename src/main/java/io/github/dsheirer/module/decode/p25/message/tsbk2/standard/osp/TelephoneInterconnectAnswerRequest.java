package io.github.dsheirer.module.decode.p25.message.tsbk2.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.identifier.integer.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.identifier.string.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.message.tsbk2.OSPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Digit;

import java.util.ArrayList;
import java.util.List;

/**
 * Group voice call channel grant.
 */
public class TelephoneInterconnectAnswerRequest extends OSPMessage
{
    private static final int[] DIGIT_1 = {16, 17, 18, 19};
    private static final int[] DIGIT_2 = {20, 21, 22, 23};
    private static final int[] DIGIT_3 = {24, 25, 26, 27};
    private static final int[] DIGIT_4 = {28, 29, 30, 31};
    private static final int[] DIGIT_5 = {32, 33, 34, 35};
    private static final int[] DIGIT_6 = {36, 37, 38, 39};
    private static final int[] DIGIT_7 = {40, 41, 42, 43};
    private static final int[] DIGIT_8 = {44, 45, 46, 47};
    private static final int[] DIGIT_9 = {48, 49, 50, 51};
    private static final int[] DIGIT_10 = {52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72,
            73, 74, 75, 76, 77, 78, 79};

    private IIdentifier mTelephoneNumber;
    private IIdentifier mTargetAddress;
    private List<IIdentifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public TelephoneInterconnectAnswerRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getTelephoneNumber());
        sb.append(" TO:").append(getTargetAddress());
        return sb.toString();
    }

    public IIdentifier getTelephoneNumber()
    {
        if(mTelephoneNumber == null)
        {
            List<Integer> digits = new ArrayList<>();
            digits.add(getMessage().getInt(DIGIT_1));
            digits.add(getMessage().getInt(DIGIT_2));
            digits.add(getMessage().getInt(DIGIT_3));
            digits.add(getMessage().getInt(DIGIT_4));
            digits.add(getMessage().getInt(DIGIT_5));
            digits.add(getMessage().getInt(DIGIT_6));
            digits.add(getMessage().getInt(DIGIT_7));
            digits.add(getMessage().getInt(DIGIT_8));
            digits.add(getMessage().getInt(DIGIT_9));
            digits.add(getMessage().getInt(DIGIT_10));

            mTelephoneNumber = APCO25TelephoneNumber.create(Digit.decode(digits));
        }

        return mTelephoneNumber;
    }

    public IIdentifier getTargetAddress()
    {
        if(mTargetAddress == null)
        {
            mTargetAddress = APCO25ToTalkgroup.createIndividual(getMessage().getInt(TARGET_ADDRESS));
        }

        return mTargetAddress;
    }

    @Override
    public List<IIdentifier> getIdentifiers()
    {
        if(mIdentifiers == null)
        {
            mIdentifiers = new ArrayList<>();
            mIdentifiers.add(getTargetAddress());
            mIdentifiers.add(getTelephoneNumber());
        }

        return mIdentifiers;
    }
}
