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
 * APCO25 Encryption Algorithm IDs.
 *
 * Note: values are from a variety of places, including from the OP-25 project.
 */
public enum Encryption
{
    ACCORDION_3(0x00, "ACCORDIAN 3"),
    BATON_AUTO_EVEN(0x01, "BATON AUTO EVEN"),
    FIREFLY_TYPE1(0x02, "FIREFLY"),
    MAYFLY_TYPE1(0x03, "MAYFLY"),
    SAVILLE(0x04, "SAVILLE"),
    MOTOROLA_PADSTONE(0x05, "MOTOROLA PADSTONE"), //from OP25
    BATON_AUTO_ODD(0x41, "BATON AUTO ODD"),
    UNENCRYPTED(0x80, "UNENCRYPTED"),
    DES_OFB(0x81, "DES OFB"),
    TRIPLE_DES_2_KEY(0x82, "2-KEY TRIPLE DES"),
    TRIPLE_DES_3_KEY(0x83, "3-KEY TRIPLE DES"),
    AES_256(0x84, "AES-256"),
    AES_128(0x85, "AES-128"),
    AES_CBC(0x88, "AES-CBC"), //from OP25

    //Below from OP25 ...
    AES_128_OFB(0x89, "AES-128-OFB"),
    DES_XL(0x9F, "MOTOROLA DES-XL"),
    DVI_XL(0xA0, "MOTOROLA DVI-XL"),
    DVP_XL(0xA1, "MOTOROLA DVP-XL"),
    DVP_SPFL(0xA2, "MOTOROLA DVP-SPFL"),
    HAYSTACK(0xA3, "MOTOROLA HAYSTACK"),
    MOTOROLA_A4(0xA4, "MOTOROLA UNKNOWN A4"),
    MOTOROLA_A5(0xA5, "MOTOROLA UNKNOWN A5"),
    MOTOROLA_A6(0xA6, "MOTOROLA UNKNOWN A6"),
    MOTOROLA_A7(0xA7, "MOTOROLA UNKNOWN A7"),
    MOTOROLA_A8(0xA8, "MOTOROLA UNKNOWN A8"),
    MOTOROLA_A9(0xA9, "MOTOROLA UNKNOWN A9"),
    MOTOROLA_ADP(0xAA, "MOTOROLA ADP 40-BIT RC4"),
    MOTOROLA_AB(0xAB, "MOTOROLA CFX-256"),
    MOTOROLA_AC(0xAC, "MOTOROLA UNKNOWN AC"),
    MOTOROLA_AD(0xAD, "MOTOROLA UNKNOWN AD"),
    MOTOROLA_AE(0xAE, "MOTOROLA UNKNOWN AE"),
    MOTOROLA_AF(0xAF, "MOTOROLA AES-256-GCM"),
    MOTOROLA_B0(0xB0, "MOTOROLA DVP B0"),
    UNKNOWN(-1, "UNKNOWN");

    private int mValue;
    private String mLabel;

    Encryption(int value, String label)
    {
        mValue = value;
        mLabel = label;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }

    /**
     * Encryption type value.
     * @return value.
     */
    public int getValue()
    {
        return mValue;
    }

    /**
     * Utility method to lookup the encryption type from the value.
     * @param value of the encryption type.
     * @return enumeration entry or UNKNOWN.
     */
    public static Encryption fromValue(int value)
    {
        switch(value)
        {
            case 0x00:
                return ACCORDION_3;
            case 0x01:
                return BATON_AUTO_EVEN;
            case 0x02:
                return FIREFLY_TYPE1;
            case 0x03:
                return MAYFLY_TYPE1;
            case 0x04:
                return SAVILLE;
            case 0x05:
                return MOTOROLA_PADSTONE;
            case 0x41:
                return BATON_AUTO_ODD;
            case 0x80:
                return UNENCRYPTED;
            case 0x81:
                return DES_OFB;
            case 0x82:
                return TRIPLE_DES_2_KEY;
            case 0x83:
                return TRIPLE_DES_3_KEY;
            case 0x84:
                return AES_256;
            case 0x85:
                return AES_128;
            case 0x9F:
                return DES_XL;
            case 0xA0:
                return DVI_XL;
            case 0xA1:
                return DVP_XL;
            case 0xA2:
                return DVP_SPFL;
            case 0xA3:
                return HAYSTACK;
            case 0xA4:
                return MOTOROLA_A4;
            case 0xA5:
                return MOTOROLA_A5;
            case 0xA6:
                return MOTOROLA_A6;
            case 0xA7:
                return MOTOROLA_A7;
            case 0xA8:
                return MOTOROLA_A8;
            case 0xA9:
                return MOTOROLA_A9;
            case 0xAA:
                return MOTOROLA_ADP;
            case 0xAB:
                return MOTOROLA_AB;
            case 0xAC:
                return MOTOROLA_AC;
            case 0xAD:
                return MOTOROLA_AD;
            case 0xAE:
                return MOTOROLA_AE;
            case 0xAF:
                return MOTOROLA_AF;
            case 0xB0:
                return MOTOROLA_B0;
            default:
                return UNKNOWN;
        }
    }
}
