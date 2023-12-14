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
    ASELSAN(9, "ASELSAN"),
    KIRISUN(10, "KIRISUN"),
    DMR_ASSOCIATION(11, "DMR ASSOCIATION"),
    SEPURA(12, "SEPURA"),
    ITALIA_RED_CROSS(13, "ITALIA RED CROSS"),
    MOTOROLA_CAPACITY_PLUS(16, "MOTOROLA CAP+"),
    MINISTERO_DELL_INTERNO(19, "ITALY MIN INTERIOR"),
    EMC_COMM_SRL_28(28, "EMC COMM SRL 28"),
    EMC_COMM_SRL_32(32, "EMC COMM SRL 32"),
    JVC_KENWOOD(51, "JVC-KENWOOD"),
    RADIO_ACTIVITY_SRL(60, "RADIO ACTIVITY SRL"),
    ZTE_TRUNKING(84, "ZTE TRUNKING"),
    TAIT(88, "TAIT"),
    HYTERA_68(104, "HYTERA"),
    VERTEX_STANDARD(119, "VERTEX STANDARD"),
    SIMOCO(120, "SIMOCO"),
    TEST(121, "TEST"),
    HYTERA_88(136, "HYTERA"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    /**
     * Constructs an instance
     * @param value decimal for the vendor
     * @param label for pretty print
     */
    Vendor(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    /**
     * Vendor ID value
     * @return value
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Pretty print label
     * @return label
     */
    public String getLabel()
    {
        return mLabel;
    }

    /**
     * Overrides default to use the label value instead.
     * @return string representation.
     */
    @Override
    public String toString()
    {
        return getLabel();
    }

    /**
     * Quick lookup map of integer values to corresponding vendor entries
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
