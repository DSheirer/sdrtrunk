/*
 * *****************************************************************************
 *  Copyright (C) 2014-2020 Dennis Sheirer
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

package io.github.dsheirer.module.decode.dmr.message.type;

import java.util.Map;
import java.util.TreeMap;

/**
 * Enumeration of DMR vendors aka Feature IDs (FID)
 */
public enum Vendor
{
    STANDARD(0, "STANDARD"),
    FYLDE_MICRO(4, "FYLDE MICRO"),
    PROD_EL_SPA(5, "PROD-EL SPA"),
    MOTOROLA_CONNECT_PLUS(6, "MOTOROLA CON+"),
    RADIO_DATA_GMBH(7, "RADIO DATA GMBH"),
    HYTERA_8(8, "HYTERA"),
    MOTOROLA_CAPACITY_PLUS(16, "MOTOROLA CAP+"),
    EMC_SPA_19(19, "EMC SPA"),
    EMC_SPA_28(28, "EMC SPA"),
    RADIO_ACTIVITY_SRL_51(51, "RADIO ACTIVITY SRL"),
    RADIO_ACTIVITY_SRL_60(60, "RADIO ACTIVITY SRL"),
    TAIT(88, "TAIT"),
    HYTERA_68(104, "HYTERA"),
    VERTEX_STANDARD(119, "VERTEX STANDARD"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    Vendor(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    public int getValue()
    {
        return mValue;
    }

    public String getLabel()
    {
        return mLabel;
    }

    @Override
    public String toString()
    {
        return getLabel();
    }

    /**
     * Map of integer values to corresponding vendor entries
     */
    private static final Map<Integer,Vendor> LOOKUP_MAP = new TreeMap<>();

    /**
     * Static loading of the lookup map
     */
    static
    {
        for(Vendor vendor: Vendor.values())
        {
            LOOKUP_MAP.put(vendor.getValue(), vendor);
        }
    }

    /**
     * Lookup the vendor from an integer value
     */
    public static Vendor fromValue(int value)
    {
        if(LOOKUP_MAP.containsKey(value))
        {
            return LOOKUP_MAP.get(value);
        }

        return UNKNOWN;
    }
}
