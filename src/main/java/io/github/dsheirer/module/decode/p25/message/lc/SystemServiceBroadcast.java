package io.github.dsheirer.module.decode.p25.message.lc;

import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.identifier.IIdentifier;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.Collections;
import java.util.List;

/**
 * Termination or cancellation of a call and release of the channel.
 */
public class SystemServiceBroadcast extends LinkControlWord
{
    private static final int[] REQUEST_PRIORITY_LEVEL = {20, 21, 22, 23};
    private static final int[] AVAILABLE_SERVICES = {24, 25, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
            45, 46, 47};
    private static final int[] SUPPORTED_SERVICES = {48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59, 60, 61,
            62, 63, 64, 65, 66, 67, 68, 69, 70, 71};

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     */
    public SystemServiceBroadcast(BinaryMessage message)
    {
        super(message);
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getMessageStub());
        sb.append(" REQUEST PRIORITY LEVEL:").append(getRequestPriorityLevel());
        sb.append(" AVAILABLE SERVICES ").append(getAvailableServices());
        sb.append(" SUPPORTED SERVICES ").append(getSupportedServices());
        return sb.toString();
    }

    /**
     * Threshold priority for service requests for this site
     */
    public int getRequestPriorityLevel()
    {
        return getMessage().getInt(REQUEST_PRIORITY_LEVEL);
    }


    public List<Service> getAvailableServices()
    {
        long bitmap = getMessage().getLong(AVAILABLE_SERVICES);
        return Service.getServices(bitmap);
    }

    public List<Service> getSupportedServices()
    {
        long bitmap = getMessage().getLong(SUPPORTED_SERVICES);
        return Service.getServices(bitmap);
    }

    /**
     * List of identifiers contained in this message
     */
    @Override
    public List<IIdentifier> getIdentifiers()
    {
        return Collections.EMPTY_LIST;
    }
}
