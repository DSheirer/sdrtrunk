package io.github.dsheirer.module.decode.p25.message.tsbk.standard.isp;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25FromTalkgroup;
import io.github.dsheirer.module.decode.p25.identifier.telephone.APCO25TelephoneNumber;
import io.github.dsheirer.module.decode.p25.message.tsbk.ISPMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.VoiceServiceOptions;

import java.util.ArrayList;
import java.util.List;

/**
 * Implicit Dialing Telephone Interconnect request.  The telephone number (PSTN) is a predefined system value and is
 * identified by the PSTN address value.
 */
public class TelephoneInterconnectPstnRequest extends ISPMessage
{
    private static final int[] SERVICE_OPTIONS = {16, 17, 18, 19, 20, 21, 22, 23};
    private static final int[] RESERVED = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42,
            43, 44, 45, 46, 47};
    private static final int[] PSTN_ADDRESS = {48, 49, 50, 51, 52, 53, 54, 55};
    private static final int[] SOURCE_ADDRESS = {56, 57, 58, 59, 60, 61, 62, 63, 64, 65, 66, 67, 68, 69, 70, 71, 72, 73,
            74, 75, 76, 77, 78, 79};

    private VoiceServiceOptions mVoiceServiceOptions;
    private Identifier mPstnAddress;
    private Identifier mSourceAddress;
    private List<Identifier> mIdentifiers;

    /**
     * Constructs a TSBK from the binary message sequence.
     */
    public TelephoneInterconnectPstnRequest(DataUnitID dataUnitId, CorrectedBinaryMessage message, int nac, long timestamp)
    {
        super(dataUnitId, message, nac, timestamp);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" FM:").append(getSourceAddress());
        sb.append(" TO:").append(getPstnAddress());
        sb.append(" ").append(getVoiceServiceOptions().toString());
        return sb.toString();
    }

    /**
     * Service options for the request
     */
    public VoiceServiceOptions getVoiceServiceOptions()
    {
        if(mVoiceServiceOptions == null)
        {
            mVoiceServiceOptions = new VoiceServiceOptions(getMessage().getInt(SERVICE_OPTIONS));
        }

        return mVoiceServiceOptions;
    }

    public Identifier getPstnAddress()
    {
        if(mPstnAddress == null)
        {
            mPstnAddress = APCO25TelephoneNumber.createTo("PSTN ENTRY #" + getMessage().getInt(PSTN_ADDRESS));
        }

        return mPstnAddress;
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
            mIdentifiers.add(getPstnAddress());
            mIdentifiers.add(getSourceAddress());
        }

        return mIdentifiers;
    }
}
