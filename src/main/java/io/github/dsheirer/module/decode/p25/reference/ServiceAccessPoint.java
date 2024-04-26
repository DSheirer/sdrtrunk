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

public enum ServiceAccessPoint
{
    UNENCRYPTED_USER_DATA("USER DATA", 0),
    ENCRYPTED_USER_DATA("ENCRYPTED USER DATA", 1),
    CIRCUIT_DATA("CIRCUIT DATA", 2),
    CIRCUIT_DATA_CONTROL("CIRCUIT DATA CONTROL", 3),
    PACKET_DATA("PACKET DATA", 4),
    ADDRESS_RESOLUTION_PROTOCOL("ADDRESS RESOLUTION PROTOCOL", 5),
    SNDCP_PACKET_DATA_CONTROL("SNDCP PACKET DATA CONTROL", 6),
    SAP_7("7", 7),
    SAP_8("8", 8),
    SAP_9("9", 9),
    SAP_10("10", 10),
    SAP_11("11", 11),
    SAP_12("12", 12),
    SAP_13("13", 13),
    SAP_14("14", 14),
    PACKET_DATA_SCAN_PREAMBLE("PACKET DATA SCAN PREAMBLE", 15),
    SAP_16("16", 16),
    SAP_17("17", 17),
    SAP_18("18", 18),
    SAP_19("19", 19),
    SAP_20("20", 20),
    SAP_21("21", 21),
    SAP_22("22", 22),
    SAP_23("23", 23),
    SAP_24("24", 24),
    SAP_25("25", 25),
    SAP_26("26", 26),
    SAP_27("27", 27),
    SAP_28("28", 28),
    PACKET_DATA_ENCRYPTION_SUPPORT("PACKET DATA ENCRYPTION SUPPORT", 29),
    SAP_30("30", 30),
    EXTENDED_ADDRESS("EXTENDED ADDRESS FOR SYMMETRIC ADDRESSING", 31),
    REGISTRATION_AND_AUTHORIZATION("REGISTRATION AND AUTHORIZATION", 32),
    CHANNEL_REASSIGNMENT("CHANNEL REASSIGNMENT", 33),
    SYSTEM_CONFIGURATION("SYSTEM CONFIGURATION", 34),
    MOBILE_RADIO_LOOPBACK("MOBILE RADIO LOOPBACK", 35),
    MOBILE_RADIO_STATISTICS("MOBILE RADIO STATISTICS", 36),
    MOBILE_RADIO_OUT_OF_SERVICE("MOBILE RADIO OUT OF SERVICE", 37),
    MOBILE_RADIO_PAGING("MOBILE RADIO PAGING", 38),
    MOBILE_RADIO_CONFIGURATION("MOBILE RADIO CONFIGURATION", 39),
    UNENCRYPTED_KEY_MANAGEMENT_MESSAGE("UNENCRYPTED KEY MANAGEMENT", 40),
    ENCRYPTED_KEY_MANAGEMENT_MESSAGE("ENCRYPTED KEY MANAGEMENT", 41),
    SAP_42("42", 42),
    SAP_43("43", 43),
    SAP_44("44", 44),
    SAP_45("45", 45),
    SAP_46("46", 46),
    SAP_47("47", 47),
    LOCATION_SERVICE("LOCATION SERVICE", 48),
    SAP_49("49", 49),
    SAP_50("50", 50),
    SAP_51("51", 51),
    SAP_52("52", 52),
    SAP_53("53", 53),
    SAP_54("54", 54),
    SAP_55("55", 55),
    SAP_56("56", 56),
    SAP_57("57", 57),
    SAP_58("58", 58),
    SAP_59("59", 59),
    SAP_60("60", 60),
    UNENCRYPTED_TRUNKING_CONTROL("TRUNKING CONTROL", 61),
    SAP_62("62", 62),
    ENCRYPTED_TRUNKING_CONTROL("ENCRYPTED TRUNKING CONTROL", 63),
    UNKNOWN("UNKN", -1);

    private String mLabel;
    private int mValue;

    private ServiceAccessPoint(String label, int value)
    {
        mLabel = label;
        mValue = value;
    }

    public String getLabel()
    {
        return mLabel;
    }

    public int getValue()
    {
        return mValue;
    }

    public static ServiceAccessPoint fromValue(int value)
    {
        if(0 <= value && value <= 63)
        {
            return values()[value];
        }

        return UNKNOWN;
    }
}
