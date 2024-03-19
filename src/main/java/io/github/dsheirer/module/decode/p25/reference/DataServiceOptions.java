/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.reference;

public class DataServiceOptions extends ServiceOptions
{
    private static final int NSAPI = 0x0F;

    public DataServiceOptions(int serviceOptions)
    {
        super(serviceOptions);
    }

    /**
     * Network Service Access Point ID
     */
    public int getNSAPI()
    {
        return (mServiceOptions & NSAPI);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("NSAPI:").append(getNSAPI());

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }
        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }
        sb.append(" ").append(getDuplex()).append(" DUPLEX");
        sb.append(" ").append(getSessionMode()).append(" MODE");
        return sb.toString();
    }
}
