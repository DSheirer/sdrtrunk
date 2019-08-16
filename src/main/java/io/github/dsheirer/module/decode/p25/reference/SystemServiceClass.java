/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.p25.reference;

import java.util.ArrayList;
import java.util.List;

public class SystemServiceClass
{
    private static final int NO_SERVICES = 0x00;

    private static final int COMPOSITE_CONTROL_CHANNEL = 0x01;
    private static final int NO_SERVICE_REQUESTS = 0x02;
    private static final int BACKUP_CONTROL_CHANNEL = 0x04;
    private static final int DATA_SERVICE = 0x10;
    private static final int VOICE_SERVICE = 0x20;
    private static final int REGISTRATION_SERVICE = 0x40;
    private static final int AUTHENTICATION_SERVICE = 0x80;

    private int mSystemServiceClass;

    public SystemServiceClass(int systemServiceClass)
    {
        mSystemServiceClass = systemServiceClass;
    }

    public List<String> getServices()
    {
        List<String> services = new ArrayList<>();

        if(hasFlag(DATA_SERVICE))
        {
            services.add("DATA");
        }
        if(hasFlag(VOICE_SERVICE))
        {
            services.add("VOICE");
        }
        if(hasFlag(REGISTRATION_SERVICE))
        {
            services.add("REGISTRATION");
        }
        if(hasFlag(AUTHENTICATION_SERVICE))
        {
            services.add("AUTHENTICATION");
        }
        if(hasFlag(COMPOSITE_CONTROL_CHANNEL))
        {
            services.add("COMPOSITE CONTROL CHANNEL");
        }
        if(hasFlag(NO_SERVICE_REQUESTS))
        {
            services.add("NO SERVICE REQUESTS");
        }
        if(hasFlag(BACKUP_CONTROL_CHANNEL))
        {
            services.add("BACKUP CONTROL CHANNEL");
        }

        return services;
    }

    private boolean hasFlag(int flag)
    {
        return (mSystemServiceClass & flag) == flag;
    }

    @Override
    public String toString()
    {
        return getServices().toString();
    }

    public static SystemServiceClass create(int systemServiceClass)
    {
        return new SystemServiceClass(systemServiceClass);
    }
}
