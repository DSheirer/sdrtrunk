/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.module.decode.p25.message.ldu.lc;

import io.github.dsheirer.module.decode.p25.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.reference.Service;

import java.util.List;

public class SystemServiceBroadcast extends LDU1Message
{
    public static final int[] REQUEST_PRIORITY_LEVEL = {384, 385, 386, 387};
    public static final int[] AVAILABLE_SERVICES = {536, 537, 538, 539, 540, 541,
        546, 547, 548, 549, 550, 551, 556, 557, 558, 559, 560, 561, 566, 567, 568, 569, 570, 571};
    public static final int[] SUPPORTED_SERVICES = {720, 721, 722, 723, 724, 725,
        730, 731, 732, 733, 734, 735, 740, 741, 742, 743, 744, 745, 750, 751, 752, 753, 754, 755};

    public SystemServiceBroadcast(LDU1Message message)
    {
        super(message);
    }

    @Override
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
