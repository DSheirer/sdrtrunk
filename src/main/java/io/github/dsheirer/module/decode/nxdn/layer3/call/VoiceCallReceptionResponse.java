package io.github.dsheirer.module.decode.nxdn.layer3.call;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.integer.IntegerIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNFullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.identifier.NXDNRadioIdentifier;
import io.github.dsheirer.module.decode.nxdn.layer3.NXDNMessageType;
import io.github.dsheirer.module.decode.nxdn.layer3.type.CauseVD;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationID;
import io.github.dsheirer.module.decode.nxdn.layer3.type.LocationIDOption;

/**
 * Voice call reception response
 */
public class VoiceCallReceptionResponse extends VoiceCall
{
    private static final IntField CAUSE_VD = IntField.length8(OCTET_7);
    private static final IntField LOCATION_ID_OPTION = IntField.length5(OCTET_8);
    private static final int LOCATION_ID = OCTET_8 + 5;
    private LocationID mLocationID;

    /**
     * Constructs an instance
     *
     * @param message   with binary data
     * @param timestamp for the message
     * @param type      of message
     */
    public VoiceCallReceptionResponse(CorrectedBinaryMessage message, long timestamp, NXDNMessageType type)
    {
        super(message, timestamp, type);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();

        if(getCallControlOption().isEmergency())
        {
            sb.append("EMERGENCY ");
        }

        if(getCallControlOption().isPriorityPaging())
        {
            sb.append("PRIORITY PAGING ");
        }

        CauseVD cause = getCause();

        if(cause == CauseVD.ACCEPTED_NORMAL)
        {
            sb.append(getCallType()).append(" VOICE CALL RECEPTION ACCEPTED");
        }
        else
        {
            sb.append(getCallType()).append(" VOICE CALL RECEPTION FAIL:").append(cause);

        }
        sb.append(" FROM:").append(getSource());
        sb.append(" TO:").append(getDestination());
        sb.append(" ").append(getEncryptionKeyIdentifier());
        sb.append(getCallOption());
        return sb.toString();
    }

    @Override
    public NXDNRadioIdentifier getSource()
    {
        if(mSourceIdentifier == null && hasLocationID() && getLocationIDOption().isSource())
        {
            mSourceIdentifier = NXDNFullyQualifiedRadioIdentifier.createFrom(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_3));
        }

        return super.getSource();
    }

    @Override
    public IntegerIdentifier getDestination()
    {
        if(mDestinationIdentifier == null && hasLocationID() && getLocationIDOption().isDestination())
        {
            mDestinationIdentifier = NXDNFullyQualifiedRadioIdentifier.createTo(getLocationID().getSystem().getValue(),
                    getMessage().getInt(IDENTIFIER_OCTET_5));
        }

        return super.getDestination();
    }

    /**
     * Location ID option
     */
    public LocationIDOption getLocationIDOption()
    {
        return LocationIDOption.fromValue(getMessage().getInt(LOCATION_ID_OPTION));
    }

    /**
     * Indicates if this message contains an optional location ID field.
     */
    public boolean hasLocationID()
    {
        return getCallControlOption().hasLocationId();
    }

    public LocationID getLocationID()
    {
        if(mLocationID == null && hasLocationID())
        {
            mLocationID = new LocationID(getMessage(), LOCATION_ID, true);
        }

        return mLocationID;
    }

    /**
     * Amplifying cause for the response.
     */
    public CauseVD getCause()
    {
        return CauseVD.fromValue(getMessage().getInt(CAUSE_VD));
    }
}
