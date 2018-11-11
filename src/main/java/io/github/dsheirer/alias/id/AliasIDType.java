/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.alias.id;

public enum AliasIDType
{
    ESN("ESN"),
    FLEETSYNC("Fleetsync"),
    LEGACY_TALKGROUP("Legacy Talkgroup"),
    LOJACK("LoJack"),
    LTR_NET_UID("LTR-Net UID"),
    MDC1200("MDC-1200"),
    MIN("Passport MIN"),
    MPT1327("MPT-1327"),
    SITE("Site"),
    STATUS("Status"),
    TALKGROUP("Talkgroup"),
    INVERT("Audio Inversion"),
    PRIORITY("Audio Priority"),
    NON_RECORDABLE("Audio Non-Recordable"),
    BROADCAST_CHANNEL("Audio Broadcast Channel");

    private String mLabel;

    private AliasIDType(String label)
    {
        mLabel = label;
    }

    public String toString()
    {
        return mLabel;
    }
}
