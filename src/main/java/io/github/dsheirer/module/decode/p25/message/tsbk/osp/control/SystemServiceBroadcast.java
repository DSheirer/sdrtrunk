package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.module.decode.p25.message.tsbk.TSBKMessage;
import io.github.dsheirer.module.decode.p25.reference.DataUnitID;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.List;

public class SystemServiceBroadcast extends TSBKMessage
{
    public static final int[] AVAILABLE_SERVICES = {88, 89, 90, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111};
    public static final int[] SUPPORTED_SERVICES = {112, 113, 114, 115, 116, 117, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134, 135};
    public static final int[] REQUEST_PRIORITY_LEVEL = {136, 137, 138, 139, 140, 141, 142, 143};

    public SystemServiceBroadcast(BinaryMessage message, DataUnitID duid, AliasList aliasList)
    {
        super(message, duid, aliasList);
    }

    public String getMessage()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getMessageStub());

        sb.append(" SERVICES AVAILABLE ");

        sb.append(getAvailableServices());

        sb.append(" SUPPORTED ");

        sb.append(getSupportedServices());

        return sb.toString();
    }

    public List<Service> getAvailableServices()
    {
        long bitmap = mMessage.getLong(AVAILABLE_SERVICES);

        return Service.getServices(bitmap);
    }

    public List<Service> getSupportedServices()
    {
        long bitmap = mMessage.getLong(SUPPORTED_SERVICES);

        return Service.getServices(bitmap);
    }
}
