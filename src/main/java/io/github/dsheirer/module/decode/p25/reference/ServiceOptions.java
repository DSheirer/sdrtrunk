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
package io.github.dsheirer.module.decode.p25.reference;

public class ServiceOptions
{
    private static final int EMERGENCY_FLAG = 0x80;
    private static final int ENCRYPTION_FLAG = 0x40;
    private static final int DUPLEX = 0x20;
    private static final int SESSION_MODE = 0x10;
    private static final int PRIORITY = 0x07;

    private int mServiceOptions;

    public ServiceOptions(int serviceOptions)
    {
        mServiceOptions = serviceOptions;
    }

    /**
     * Indicates if the channel is specially processed as an emergency service.
     */
    public boolean isEmergency()
    {
        return isSet(EMERGENCY_FLAG);
    }

    /**
     * Indicates if the channel is encrypted for the session.
     */
    public boolean isEncrypted()
    {
        return isSet(ENCRYPTION_FLAG);
    }

    /**
     * Duplex mode for the session.  Half duplex indicates that only one entity can communicate
     * at a time.  Full duplex indicates that both entities can communicate simultaneously.
     */
    public Duplex getDuplex()
    {
        return isSet(DUPLEX) ? Duplex.FULL : Duplex.HALF;
    }

    /**
     * Network mode for the session, packet (data) or circuit (voice).
     */
    public SessionMode getSessionMode()
    {
        return isSet(SESSION_MODE) ? SessionMode.PACKET : SessionMode.CIRCUIT;
    }

    /**
     * Priority, 1 - 7, assigned to the channel for the session.
     *
     * 0 = Reserved
     * 1 = Lowest priority
     * ...
     * 4 = Default
     * ...
     * 7 = Highest priority
     *
     * @return  priority for the session
     */
    public int getPriority()
    {
        return (mServiceOptions & PRIORITY);
    }

    /**
     * Indicates if the bit(s) referenced by the mask is set in the service options value.
     *
     * @param mask to check for set bits
     * @return true if the masked bit value(s) are all set.
     */
    private boolean isSet(int mask)
    {
        return (mServiceOptions & mask) == mask;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("PRI").append(getPriority());

        if(isEncrypted())
        {
            sb.append(" ENCRYPTED");
        }

        if(isEmergency())
        {
            sb.append(" EMERGENCY");
        }

        sb.append(" ").append(getSessionMode());

        return sb.toString();
    }
}
