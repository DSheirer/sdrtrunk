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

package io.github.dsheirer.module.decode.edacs;

/**
 * EDACS message types.
 *
 * <p>Mirrors the dispatch table in DSD-FME {@code edacs-fme.c}. EDACS
 * control channel messages are 28 bits; the top 5 bits are {@code MT1}
 * which selects the major dispatch; the next 4 bits are {@code MT2}
 * which is used when {@code MT1 == 0x1F}. Values are reformatted /
 * renamed from the EDACS-EA reference (US patent US7546135B2) for
 * clarity.</p>
 */
public enum EDACSMessageType
{
    /** Voice group call grant (analog or digital, MT1 = 0x06 or 0x03) */
    GROUP_CALL("Group Call"),
    /** Voice group call grant - digital (ProVoice) only (MT1 = 0x03) */
    DIGITAL_GROUP_CALL("Digital Group Call"),
    /** Voice group call grant - analog FM only (MT1 = 0x06) */
    ANALOG_GROUP_CALL("Analog Group Call"),
    /** Voice group call grant - TDMA (MT1 = 0x01) */
    TDMA_GROUP_CALL("TDMA Group Call"),
    /** Voice individual call grant (MT1 = 0x10) */
    INDIVIDUAL_CALL("Individual Call"),
    /** Voice system-wide all-call grant (MT1 = 0x16) */
    ALL_CALL("All Call"),
    /** Radio registration / login (MT1 = 0x19) */
    LOGIN("Login"),
    /** Initiate test call (MT1 = 0x1F, MT2 = 0x0) */
    TEST_CALL("Test Call"),
    /** Adjacent site announcement (MT1 = 0x1F, MT2 = 0x1) */
    ADJACENT_SITE("Adjacent Site"),
    /** System information broadcast (MT1 = 0x1F, MT2 = 0x8) */
    SYSTEM_INFO("System Info"),
    /** Extended addressing - site ID and area (MT1 = 0x1F, MT2 = 0xA) */
    EXTENDED_ADDRESSING("Extended Addressing"),
    /** System dynamic regroup plan bitmap (MT1 = 0x1F, MT2 = 0xB) */
    DYNAMIC_REGROUP_PLAN("Regroup Plan"),
    /** System dynamic regroup detail (MT1 = 0x1F, MT2 = 0xC) */
    DYNAMIC_REGROUP_DETAIL("Dynamic Regroup"),
    /** Generic status / message broadcast (MT1 = 0x1F/0x4, 0x1F/0x7, 0x12) */
    STATUS("Status"),
    /** Unit enable / disable (MT1 = 0x1F, MT2 = 0x7) */
    UNIT_ENABLE_DISABLE("Unit Enable/Disable"),
    /** Status / message (MT1 = 0x1F, MT2 = 0x4) */
    STATUS_MESSAGE("Status/Message"),
    /** Data group call (MT1 = 0x02) */
    DATA_GROUP_CALL("Data Group Call"),
    /** Channel assignment data (MT1 = 0x12) */
    CHANNEL_ASSIGNMENT("Channel Assignment"),
    /** Unknown / unrecognized message */
    UNKNOWN("Unknown"),
    /** Idle / empty slot */
    IDLE("Idle");

    private String mLabel;

    EDACSMessageType(String label)
    {
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
