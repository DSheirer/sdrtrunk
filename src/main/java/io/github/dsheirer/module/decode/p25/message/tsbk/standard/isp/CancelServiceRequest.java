package io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.message.tsbk.ISPMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.Opcode;
import io.github.dsheirer.module.decode.p25.reference.CancelReason;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Direction;

import java.util.ArrayList;
import java.util.List;

/**
 * Cancel Service Request
 */
public class CancelServiceRequest extends ISPMessage
{
    private static final int ADDITIONAL_INFORMATION_VALID_FLAG = 16;
    private static final int[] SERVICE_TYPE = {18, 19, 20, 21, 22, 23};
    private static final int[] REASON_CODE = {24, 25, 26, 27, 28, 29, 30, 31};
    private static final int[] ADDITIONAL_INFORMATION = {32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44, 45, 46, 47,
        48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
        74, 75, 76, 77, 78, 79};

    private CancelReason mCancelReason;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public CancelServiceRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" REASON:").append(getCancelReason());
        return sb.toString();
    }

    public CancelReason getCancelReason()
    {
        if(mCancelReason == null)
        {
            mCancelReason = CancelReason.fromCode(getMessage().getInt(REASON_CODE));
        }

        return mCancelReason;
    }

    /**
     * Service type for the cancel request
     */
    public Opcode getServiceType()
    {
        return Opcode.fromValue(getMessage().getInt(SERVICE_TYPE), Direction.INBOUND, getVendor());
    }

    /**
     * Additional details about the cancel request
     */
    public String getAdditionalInformation()
    {
        return getMessage().getHex(ADDITIONAL_INFORMATION, 6);
    }

    /**
     * Indicates if the additional information field contains information
     */
    public boolean hasAdditionalInformation()
    {
        return getMessage().get(ADDITIONAL_INFORMATION_VALID_FLAG);
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
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
