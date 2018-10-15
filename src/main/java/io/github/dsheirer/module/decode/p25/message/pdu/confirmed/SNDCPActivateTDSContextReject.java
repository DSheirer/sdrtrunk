package io.github.dsheirer.module.decode.p25.message.pdu.confirmed;

import io.github.dsheirer.module.decode.p25.reference.SNDCPActivationRejectReason;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SNDCPActivateTDSContextReject extends PDUConfirmedMessage
{
    public final static Logger mLog =
            LoggerFactory.getLogger(SNDCPActivateTDSContextReject.class);

    public static final int[] NSAPI = {180, 181, 182, 183};
    public static final int[] REJECT_CODE = {184, 185, 186, 187, 188, 189, 190, 191};

    public SNDCPActivateTDSContextReject(PDUConfirmedMessage message)
    {
        super(message);
    }

    @Override
    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append("NAC:");
        sb.append(getNAC());
        sb.append(" PDUC LLID:");
        sb.append(getLogicalLinkID());
        sb.append(" REJECT SNDCP PACKET DATA ACTIVATE - REASON:");
        sb.append(getReason().getLabel());
        sb.append(mMessage.toString());

        return sb.toString();
    }

    /**
     * Network Service Access Point Identifier - up to 14 NSAPI's can be
     * allocated to the mobile with each NSAPI to be used for a specific
     * protocol layer.
     */
    public int getNSAPI()
    {
        return mMessage.getInt(NSAPI);
    }

    public SNDCPActivationRejectReason getReason()
    {
        return SNDCPActivationRejectReason.fromValue(mMessage.getInt(REJECT_CODE));
    }
}
