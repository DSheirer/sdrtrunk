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

public class VoiceServiceOptions extends ServiceOptions
{
    private static final int PRIORITY = 0x07;

    public VoiceServiceOptions(int serviceOptions)
    {
        super(serviceOptions);
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

    /**
     * Creates an instance with the encryption flag set.
     */
    public static VoiceServiceOptions createEncrypted()
    {
        return new VoiceServiceOptions(ENCRYPTION_FLAG + 4);
    }

    /**
     * Creates an instance where the encryption flag is not set.
     */
    public static VoiceServiceOptions createUnencrypted()
    {
        return new VoiceServiceOptions(4);
    }
}
