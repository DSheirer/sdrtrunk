/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.module.decode.p25.reference;

public enum Encryption
{
    ACCORDION_3(0x00, "ACCORDIAN 3"),
    BATON_AUTO_EVEN(0x01, "BATON AUTO EVEN"),
    FIREFLY_TYPE1(0x02, "FIREFLY"),
    MAYFLY_TYPE1(0x03, "MAYFLY"),
    SAVILLE(0x04, "SAVILLE"),
    BATON_AUTO_ODD(0x41, "BATON AUTO ODD"),
    UNENCRYPTED(0x80, "UNENCRYPTED"),
    DES_OFB(0x81, "DES OFB"),
    TRIPLE_DES_2_KEY(0x82, "TRIPLE DES 2"),
    TRIPLE_DES_3_KEY(0x83, "TRIPLE DES 3"),
    AES_256(0x84, "AES-256"),
    AES_CBC(0x85, "AES-CBC"),
    DES_XL(0x9F, "DES-XL"), /* Motorola Proprietary */
    DVI_XL(0xA0, "DVI-XL"), /* Motorola Proprietary */
    DVP_XL(0xA1, "DVP-XL"), /* Motorola Proprietary */
    ADP(0xAA, "ADP"),
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
                return AES_CBC;
            case 0x9F:
                return DES_XL;
            case 0xA0:
                return DVI_XL;
            case 0xA1:
                return DVP_XL;
            case 0xAA:
                return ADP;
            default:
                return UNKNOWN;
        }

    }
}
