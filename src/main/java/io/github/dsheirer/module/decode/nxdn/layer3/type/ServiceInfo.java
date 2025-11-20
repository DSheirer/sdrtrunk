/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.nxdn.layer3.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Services information field.
 *
 * Note: name is truncated so that it doesn't collide with the ServiceInformation message.
 */
public class ServiceInfo extends Option
{
    /**
     * Constructs an instance
     * @param value of the field
     */
    public ServiceInfo(int value)
    {
        super(value);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SERVICES ").append(getServices());
        return sb.toString();
    }

    /**
     * List of services available at the site.
     */
    public List<Service> getServices()
    {
        List<Service> services = new ArrayList<>();

        //Check each service to see if the flag for that service is set in the flags field
        for(Service service: Service.values())
        {
            if(isSet(service.getValue()))
            {
                services.add(service);
            }
        }

        services.sort(Comparator.comparing(Service::toString));
        return services;
    }
}
