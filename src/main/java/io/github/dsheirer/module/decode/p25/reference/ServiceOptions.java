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

/**
 * Base channel allocation service options
 */
public abstract class ServiceOptions
{
    protected static final int EMERGENCY_FLAG = 0x80;
    public static final int ENCRYPTION_FLAG = 0x40;
    protected static final int DUPLEX = 0x20;
    protected static final int SESSION_MODE = 0x10;
    protected int mServiceOptions;

    /**
     * Constructs an instance
     * @param serviceOptions
     */
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
     * Indicates if this session mode is for packet
     */
    public boolean isPacket()
    {
        return getSessionMode() == SessionMode.PACKET;
    }

    /**
     * Indicates if this session mode is for voice
     */
    public boolean isVoice()
    {
        return getSessionMode() == SessionMode.CIRCUIT;
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
}
