package io.github.dsheirer.module.decode.p25.message.tsbk.standard.osp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25ToTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.OSPMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.DenyReason;
import io.github.dsheirer.module.decode.p25.reference.Direction;
import io.github.dsheirer.module.decode.p25.reference.Vendor;

import java.util.ArrayList;
import java.util.List;

/**
 * Deny response
 */
public class DenyResponse extends OSPMessage
{
    private static final int ADDITIONAL_INFORMATION_FLAG = 16;
    private static final int[] SERVICE_TYPE = {18, 19, 20, 21, 22, 23};
    private static final int[] REASON = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ADDITIONAL_INFO = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47, 48,
            49, 50, 51, 52, 53, 54, 55};
    private static final int[] TARGET_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private DenyReason mDenyReason;
    private String mAdditionalInfo;
    private Identifier mTargetAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public DenyResponse(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" TO:").append(getTargetAddress());
        sb.append(" SERVICE:").append(getDeniedServiceType());
        sb.append(" REASON:").append(getDenyReason());

        if(hasAdditionalInformation())
        {
            sb.append(" INFO:").append(getAdditionalInfo());
        }

        return sb.toString();
    }

    private boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_FLAG);
    }

    public String getAdditionalInfo()
    {
        if(mAdditionalInfo == null)
        {
            mAdditionalInfo = getMessage().getHex(ADDITIONAL_INFO, 6);
        }

        return mAdditionalInfo;
    }

    /**
     * Opcode representing the service type that is being acknowledged by the radio unit.
     */
    public Opcode getDeniedServiceType()
    {
        return Opcode.fromValue(getMessage().getInt(SERVICE_TYPE), Direction.OUTBOUND, Vendor.STANDARD);
    }

    public DenyReason getDenyReason()
    {
        if(mDenyReason == null)
        {
            mDenyReason = DenyReason.fromCode(getMessage().getInt(REASON));
        }

        return mDenyReason;
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
