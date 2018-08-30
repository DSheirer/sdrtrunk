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
package io.github.dsheirer.module.decode.p25.message.tsbk.osp.control;

public class SystemService
{
    public static boolean isCompositeControlChannel(int service)
    {
        return (service & 0x01) == 0x01;
    }

    public static boolean isUpdateControlChannelOnly(int service)
    {
        return (service & 0x02) == 0x02;
    }

    public static boolean isBackupControlChannelOnly(int service)
    {
        return (service & 0x04) == 0x04;
    }

    public static boolean providesDataServiceRequests(int service)
    {
        return (service & 0x10) == 0x10;
    }

    public static boolean providesVoiceServiceRequests(int service)
    {
        return (service & 0x20) == 0x20;
    }

    public static boolean providesRegistrationServices(int service)
    {
        return (service & 0x40) == 0x40;
    }

    public static boolean providesAuthenticationServices(int service)
    {
        return (service & 0x80) == 0x80;
    }

    public static String toString(int service)
    {
        StringBuilder sb = new StringBuilder();

        sb.append("CONTROL CHAN[");

        if(isCompositeControlChannel(service))
        {
            sb.append(" COMPOSITE");
        }

        if(isUpdateControlChannelOnly(service))
        {
            sb.append(" UPDATE");
        }

        if(isBackupControlChannelOnly(service))
        {
            sb.append(" BACKUP");
        }

        sb.append(" ] SERVICES[");

        if(providesAuthenticationServices(service))
        {
            sb.append(" AUTHENTICATION");
        }

        if(providesDataServiceRequests(service))
        {
            sb.append(" DATA");
        }

        if(providesRegistrationServices(service))
        {
            sb.append(" REGISTRATION");
        }

        if(providesVoiceServiceRequests(service))
        {
            sb.append(" VOICE");
        }

        sb.append(" ]");

        return sb.toString();
    }
}
