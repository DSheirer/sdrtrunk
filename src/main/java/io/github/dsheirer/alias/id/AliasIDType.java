/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.alias.id;

import java.util.EnumSet;

public enum AliasIDType
{
    BROADCAST_CHANNEL("Audio Broadcast Channel"),
    DCS("Digital Coded Squelch (DCS)"),
    ESN("ESN"),
    INVERT("Audio Inversion"),
    LOJACK("LoJack"),
    LTR_NET_UID("LTR-Net UID"),
    MIN("Passport MIN"),
    P25_FULLY_QUALIFIED_RADIO_ID("P25 Fully Qualified Radio ID"),
    P25_FULLY_QUALIFIED_TALKGROUP("P25 Fully Qualified Talkgroup"),
    PRIORITY("Audio Priority"),
    RADIO_ID("Radio ID"),
    RADIO_ID_RANGE("Radio ID Range"),
    RECORD("Record"),
    SITE("Site"),
    STATUS("User Status"),
    TONES("Tone Sequence"),
    UNIT_STATUS("Unit Status"),
    TALKGROUP("Talkgroup"),
    TALKGROUP_RANGE("Talkgroup Range"),

    //Legacy identifier types - no longer used
    FLEETSYNC("Fleetsync"),
    LEGACY_TALKGROUP("Legacy Talkgroup"),
    MDC1200("MDC-1200"),
    MPT1327("MPT-1327"),
    NON_RECORDABLE("Audio Non-Recordable");

    private String mLabel;

    AliasIDType(String label)
    {
        mLabel = label;
    }

    //Values used by the View-By alias editor
    public static EnumSet<AliasIDType> VIEW_BY_VALUES = EnumSet.of(TALKGROUP, TALKGROUP_RANGE, RADIO_ID, RADIO_ID_RANGE,
        P25_FULLY_QUALIFIED_RADIO_ID, P25_FULLY_QUALIFIED_TALKGROUP, UNIT_STATUS, STATUS, TONES);

    public String toString()
    {
        return mLabel;
    }
}
